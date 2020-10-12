package com.github.fabriciolfj.estudowebflux;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@Slf4j
/**
 * Reactive Streams é um padrão
 * 1 . Asynchronous
 * 2 . Non-blocking
 * 3. Backpressure
 * Interfaces
 * Publisher -> vai emetir os eventos, ele é do tipo cold
 * Subscriptions -> é o processo quando o subscriber se "inscreve" no publisher.
 * Subscriber -> quem se inscreve
 * ==== Processo:
 * Publisher <- (subscribe) Subscriber
 * Subscriptions is created
 * Publisher (onSubscribe with the subscription) -> Subscriber
 * Subscription (por ele vou controlar o backpressure) <-(request N) Subscriber
 * Publisher -> onNext (vai chamar o onnext) Subscriber
 * until:
 * 1. Publisher send all the objects requested. (manda a quantidade de objetos requeridos, defino o backpressure)
 * 2. Publisher envia todos os objetos (quando o subscriber não define o backpressure), então o fluxo e concluido (onComplete é chamado) e o subscriber, subscription, serão cancelados
 * 3. Quando ocorre um erro. (onError) o subscriber e subscription, serão cancelados
 */
public class MonoTest {

    //https://github.com/reactor/BlockHound
    @BeforeAll
    public static void setup() {
        BlockHound.install(); //testar se nao estamos bloqueando alguma thread.
    }

    @Test
    public void blockHoundWorks() {
        Mono.delay(Duration.ofSeconds(1))
                .doOnNext(it -> {
                    try {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });//.block();
    }

    @Test
    public void monoSubscriber() {
        final String name = "Fabricio Jacob";
        final Mono<String> mono = Mono.just(name)
                .log();

        mono.subscribe();
        log.info("----------------");
        StepVerifier.create(mono)
                .expectNext("Fabricio Jacob")
                .verifyComplete();
    }

    @Test
    public void monoSubscriberConsumer() {
        final String name = "Fabricio Jacob";
        final Mono<String> mono = Mono.just(name)
                .log();

        mono.subscribe(s -> log.info("Value {}", s));
        log.info("----------------");
        StepVerifier.create(mono)
                .expectNext("Fabricio Jacob")
                .verifyComplete();
    }

    @Test
    public void monoSubscriberConsumerError() {
        final String name = "Fabricio Jacob";
        final Mono<String> mono = Mono.just(name)
                .map(s -> {
                    throw new RuntimeException("Testing mono with error");
                });

        mono.subscribe(s -> log.info("Value {}", s), e -> log.error("Something bad happened"));
        mono.subscribe(s -> log.info("Value {}", s), Throwable::printStackTrace);

        log.info("----------------");
        StepVerifier.create(mono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    public void monoSubscriberConsumerComplete() {
        final String name = "Fabricio Jacob";
        final Mono<String> mono = Mono.just(name)
                .log()
                .map(s -> s.toUpperCase());

        mono.subscribe(s -> log.info("Value {}", s), Throwable::printStackTrace, () -> log.info("Finished!"));
        log.info("----------------");
        StepVerifier.create(mono)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    /*
    * Subscription é o relacionamento do subscriber com o publisher
    * */
    @Test
    public void monoSubscriberConsumerSubscription() {
        final String name = "Fabricio Jacob";
        final Mono<String> mono = Mono.just(name)
                .log()
                .map(s -> s.toUpperCase());

        mono.subscribe(s -> log.info("Value {}", s)
                , Throwable::printStackTrace
                , () -> log.info("Finished!")
                ,subscription -> subscription.request(5));// se ele emitir 300, vou querer 5
        //Subscription::cancel); // da um clean em tudo que foi feito
        log.info("----------------");

        StepVerifier.create(mono)
                .expectNext(name.toUpperCase())
                .verifyComplete();
    }

    @Test
    public void monoDoOnMethods() {
        final String name = "Fabricio Jacob";
        final Mono<Object> mono = Mono.just(name)
                .log()
                .map(s -> s.toUpperCase())
                .doOnSubscribe(subscription -> log.info("Subscriber"))  //vai executar o subscribe do subscribe
                .doOnRequest(longNumber -> log.info("Request received {}", longNumber)) // vai emitir a quantidade configurada no backpressured.
                .doOnNext(s -> log.info("Value is here, Executing doOnNext {}", s)) //vai disparar todas as vezes que o publisher emitir um evento
                .flatMap(s -> Mono.empty())
                .doOnNext(s -> log.info("Value is here, Executing doOnNext2 {}", s)) //essa linha nao vai ser executada
                .doOnSuccess(s -> log.info("doOnSuccess executed {}", s)); //vai ser executado quando concluir o fluxo, antes do onComplete

        mono.subscribe(s -> log.info("Value {}", s), Throwable::printStackTrace, () -> log.info("Finished!"));
        log.info("----------------");

        /*StepVerifier.create(mono)
                .expectNext(name.toUpperCase())
                .verifyComplete();*/
    }

    @Test
    public void monoDoOnError() {
        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal argument exception"))
                .doOnError(e -> log.error("Error message: {}", e.getMessage()))
                .doOnNext(s -> log.info("Executing this doOnNext")) //não vai continuar, por causa do erro
                .log();

        StepVerifier.create(error)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    public void monoDoOnErrorResume() { //posso usar como fallback, usar algum mecanismo de retry
        final String name = "Fabricio Jacob";
        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal argument exception"))
                .doOnError(e -> log.error("Error message: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Inside on error resume");
                    return Mono.just(name);

                }) //ele vai continuar
                .doOnError(e -> log.error("Error message: {}", e.getMessage()))
                .doOnNext(s -> log.info("Executing this doOnNext"))
                .log();

        StepVerifier.create(error)
                .expectNext(name)
                .verifyComplete();
    }

    @Test
    public void monoDoOnErrorReturn() { //posso usar retornar um valor default por exemplo em caso de erro
        final String name = "Fabricio Jacob";
        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal argument exception"))
                .onErrorReturn("Empty") //ele vai retornar e o restante embaixo nao vai ser executado
                .doOnError(e -> log.error("Error message: {}", e.getMessage()))
                .doOnNext(s -> log.info("Executing this doOnNext"))
                .log();

        StepVerifier.create(error)
                .expectNext("Empty")
                .verifyComplete();
    }


}
