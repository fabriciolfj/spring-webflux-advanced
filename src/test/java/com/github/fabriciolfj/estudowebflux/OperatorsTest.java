package com.github.fabriciolfj.estudowebflux;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class OperatorsTest {

    @BeforeAll
    public static void setup() {
        BlockHound.install(builder -> builder.allowBlockingCallsInside("", "")); //testar se nao estamos bloqueando alguma thread, posso passar o metodo e  a classe que quero ignorar
    }

    @Test
    public void subscriberOnSimple() { //uma forma de usar de forma mono thread (ja e padrao, ele usa a main thread) ou multiple threads
        final Flux<Integer> flux = Flux.range(1,4)
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())//indica a forma de uso das threads, o subscriberon afeta todo osubscriber, todo o processo está sendo executado na thread main
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    public void publisherOnSimple() { //uma forma de usar de forma mono thread (ja e padrao, ele usa a main thread) ou multiple threads
        final Flux<Integer> flux = Flux.range(1,4)
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic())//indica a forma de uso das threads, afeta o que está abaixo dele, acima e uma thread e abaixo outra
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .then(flux::subscribe)
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    public void multipleSubscriberOnSimple() {  //evite ter 2 subscriberon
        final Flux<Integer> flux = Flux.range(1,4)
                .subscribeOn(Schedulers.boundedElastic())// o primeiro subscriberOn será o dominante
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    public void multiplePublisherOnSimple() {
        final Flux<Integer> flux = Flux.range(1,4)
                .publishOn(Schedulers.boundedElastic())// o primeiro publisherOn será em uma thread
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic()) // o segundo publisherOn será em outra thread
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    public void multiplePublisherSubscriber() {  //evite misturar publisheron com subscriberOn
        final Flux<Integer> flux = Flux.range(1,4)
                .subscribeOn(Schedulers.single())// afeta ate antes do publisheron
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .publishOn(Schedulers.boundedElastic()) //afeta o que tem abaixo
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test
    public void multiplePublisherSubscriberInvertido() {  //evite misturar publisheron com subscriberOn
        final Flux<Integer> flux = Flux.range(1,4)
                .publishOn(Schedulers.single()) //publisheron vai ser dominante, vai dar preferencia a este
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(i -> {
                    log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                    return i;
                });

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext(1,2,3,4)
                .verifyComplete();
    }

    @Test //indicado para chamadas externas a api rest por exemplo, arquivos
    public void subscriberOnIO() throws InterruptedException {
        Mono<List<String>>  list = Mono.fromCallable(() -> Files.readAllLines(Path.of("text-file"))) //vai chamar o processo que está bloqueando a thread, e vai chamar em background
            .log()
                .subscribeOn(Schedulers.boundedElastic());

        list.subscribe(s -> log.info("{}", s));

        //Thread.sleep(3000);

        StepVerifier.create(list)
                .expectSubscription()
                .thenConsumeWhile(l -> {
                    Assertions.assertFalse(l.isEmpty());
                    log.info("Size {}", l.size());
                    return true;
                })
        .verifyComplete();
    }

    @Test
    public void switchIfEmptyOperator() {
        Flux<Object> flux = emptyFlux().switchIfEmpty(Flux.just("Not empty anymore"))
                .log();

        StepVerifier.create(flux)
                .expectSubscription()
                .expectNext("Not empty anymore")
                .expectComplete()
                .verify();
    }

    @Test
    public void deferOperator() throws Exception { //vai adiar a execução, ele vai executar a cada subscriber efetuar a inscrição
        Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));
        defer.subscribe(l -> log.info("Time: {}", l));
        Thread.sleep(100);

        defer.subscribe(l -> log.info("Time: {}", l));
        Thread.sleep(100);

        defer.subscribe(l -> log.info("Time: {}", l));
        Thread.sleep(100);

        defer.subscribe(l -> log.info("Time: {}", l));
        Thread.sleep(100);

        AtomicLong atomicLong = new AtomicLong();
        defer.subscribe(atomicLong::set);

        assertTrue(atomicLong.get() > 0);

    }

    private Flux<Object> emptyFlux() {
        return Flux.empty();
    }

    @Test
    public void concatOperator() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> flux3 = Flux.concat(flux1, flux2).log();

        StepVerifier.create(flux3)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .expectComplete()
                .verify();
    }

    @Test
    public void contatWith() {
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> flux3 = flux1.concatWith(flux2);
        flux3.log();

        StepVerifier.create(flux3)
                .expectSubscription()
                .expectNext("a", "b", "c", "d")
                .expectComplete()
                .verify();
    }

    @Test
    public void combineLatest() { //combinar o ultimo dado emitido do primeiro flux, com o segundo flux
        Flux<String> flux1 = Flux.just("a", "b", "c");
        Flux<String> flux2 = Flux.just("d", "e", "f");

        Flux<String> flux3 = Flux.combineLatest(flux1,flux2, (s1,s2) -> s1.toUpperCase() + s2.toUpperCase())
                //.delayElements(Duration.ofSeconds(1))
                .log();
        flux3.log();

        flux3.subscribe(s -> log.info(s));

        StepVerifier.create(flux3)
                .expectSubscription()
                .expectNext("CD", "CE", "CF")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeOperator() { //ele nao vai esperar o flux1 emtir os eventos para combinar os elementos.
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.merge(flux1, flux2)
                .delayElements(Duration.ofMillis(200))
                .log();

        //mergeFlux.subscribe(log::info);

        StepVerifier
                .create(mergeFlux)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .expectComplete()
                .verify();
    }

    @Test
    public void mergeWithOperator() { //posso  partir de um flux existente, restante e igual ao merge comum
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = flux1.mergeWith(flux2)
                .delayElements(Duration.ofMillis(200))
                .log();

        //mergeFlux.subscribe(log::info);

        StepVerifier.create(mergeFlux)
                .expectSubscription()
                .expectNext("c", "d", "a", "b")
                .verifyComplete();
    }


    @Test
    public void mergeSequencialOperator() { //posso  partir de um flux existente, restante e igual ao merge comum
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.mergeSequential(flux1, flux2, flux1)
                .delayElements(Duration.ofMillis(200))
                .log();

        //mergeFlux.subscribe(log::info);

        StepVerifier.create(mergeFlux)
                .expectSubscription()
                .expectNext("a", "b", "c", "d", "a", "b")
                .verifyComplete();
    }

    @Test
    public void concatOperatorError() { //adiar o error
        Flux<String> flux1 = Flux.just("a", "b")
                .map(s -> {
                    if(s.equals("b")) {
                        throw new IllegalArgumentException();
                    }

                    return s;
                });

        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> flux3 = Flux.concatDelayError(flux1, flux2).log();

        StepVerifier.create(flux3)
                .expectSubscription()
                .expectNext("a", "c", "d")
                .expectError()
                .verify();
    }

    @Test
    public void mergeDelayErrorOperator() { //adiar um error
        Flux<String> flux1 = Flux.just("a", "b")
                .map(s -> {
                    if(s.equals("b")) {
                        throw new IllegalArgumentException();
                    }

                    return s;
                }).doOnError(t -> log.error("We could do something with this"));

        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.mergeDelayError(1, flux1, flux2, flux1)
                .log();

        //mergeFlux.subscribe(log::info);

        StepVerifier.create(mergeFlux)
                .expectSubscription()
                .expectNext("a",  "c", "d", "a")
                .expectError()
                .verify();
    }

    @Test
    public void flatMapOperator() throws Exception { //extrair um resultado de dentro de um flux, ou seja, achatar e ele não garanta a ordem
        Flux<String> flux = Flux.just("a", "b");

        Flux<String> names = flux.map(String::toUpperCase)
                .flatMap(this::findByName)
                .log();

        //names.subscribe(log::info);

        StepVerifier.create(names)
                .expectSubscription()
                .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
                .verifyComplete();

    }

    @Test
    public void flatMapSequencialOperator() throws Exception { //extrair um resultado de dentro de um flux, ou seja, achatar e ele garante a ordem
        Flux<String> flux = Flux.just("a", "b").delayElements(Duration.ofMillis(200));

        Flux<String> names = flux.map(String::toUpperCase).delayElements(Duration.ofMillis(100))
                .flatMapSequential(this::findByName)
                .log();

        //names.subscribe(log::info);

        StepVerifier.create(names)
                .expectSubscription()
                .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
                .verifyComplete();

    }


    public Flux<String> findByName(String name) {
        return name.equals("A") ? Flux.just("nameA1", "nameA2") : Flux.just("nameB1", "nameB2");
    }

    @Test
    public void zipOperator() { //VAI COMBINAR O PRIMEIRO ELEMENTO DE CADA FLUX
        Flux<String> title = Flux.just("Grand blue", "baki");
        Flux<String> studio = Flux.just("Zero-G", "TMS");
        Flux<Integer> episodes = Flux.just(123, 23);

        Flux<Anime> animes = Flux.zip(title, studio, episodes)
            .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), tuple.getT2(), tuple.getT3())));


        animes.subscribe(a -> log.info(a.toString()));
        StepVerifier.create(animes)
                .expectSubscription()
                .expectNext(new Anime("Grand blue", "Zero-G", 123), new Anime("baki", "TMS", 23))
                .expectComplete();
    }

    @Test
    public void zipWithOperator() { //igual acima, mas se limita a 2 publishers
        Flux<String> title = Flux.just("Grand blue", "baki");
        Flux<String> studio = Flux.just("Zero-G", "TMS");

        Flux<Anime> animes = title.zip(title, studio)
                .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), tuple.getT2(), 0)));


        animes.subscribe(a -> log.info(a.toString()));
        StepVerifier.create(animes)
                .expectSubscription()
                .expectNext(new Anime("Grand blue", "Zero-G", 0), new Anime("baki", "TMS", 0))
                .expectComplete();
    }

    @Data
    @AllArgsConstructor
    @EqualsAndHashCode
    class Anime {
        private String title;
        private String studio;
        private int episodes;
    }

}
