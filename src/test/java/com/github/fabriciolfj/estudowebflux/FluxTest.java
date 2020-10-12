package com.github.fabriciolfj.estudowebflux;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

@Slf4j
public class FluxTest {

    @BeforeAll
    public static void setup() {
        BlockHound.install(); //testar se nao estamos bloqueando alguma thread.
    }

    @Test
    public void fluxSubscriber() {
        final Flux<String> names = Flux.just("Fabricio", "Lucas", "Felicio", "Jacob", "Suzana")
                .log();

        StepVerifier.create(names)
                .expectNext("Fabricio", "Lucas", "Felicio", "Jacob", "Suzana")
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberNumbers() {
        final Flux<Integer> numbers = Flux.range(1, 5).log();

        numbers.subscribe(i -> log.info("Number {}", i));

        StepVerifier.create(numbers)
                .expectNext(1,2,3,4,5)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberFromList() {
        final Flux<Integer> numbers = Flux.fromIterable(List.of(1,2,3,4,5)).log();

        numbers.subscribe(i -> log.info("Number {}", i));

        StepVerifier.create(numbers)
                .expectNext(1,2,3,4,5)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersError() {
        final Flux<Integer> numbers = Flux.fromIterable(List.of(1,2,3,4,5))
                .log()
                .map(i -> {
                    if (i == 4) {
                        throw new IndexOutOfBoundsException("index error");
                    }

                    return i;
                });

        numbers.subscribe(i -> log.info("Number {}", i)
                , Throwable::printStackTrace
                ,() -> log.info("DONE!")
                , subscription -> subscription.request(3)); // ele pode emitir 5, mas so quero 3, consequentemente não lançará a exceção.

        StepVerifier.create(numbers)
                .expectNext(1,2,3)
                .expectError(IndexOutOfBoundsException.class)
                .verify();
    }

    @Test
    public void fluxSubscriberNumbersUglyBackpressure() {
        final Flux<Integer> numbers = Flux.fromIterable(List.of(1,2,3,4,5,6,7,8,9,10))
                .log();

        numbers.subscribe(new Subscriber<>() { //vamos criar um backpressure, onde vamos retornar elementos 2 em 2
            private int count = 0;
            private Subscription subscription;
            private int requestCount = 2;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                this.subscription.request(2);
            }

            @Override
            public void onNext(Integer integer) {
                count++;

                if (count >= requestCount) {
                    count = 0;
                    subscription.request(requestCount);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        StepVerifier.create(numbers)
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersNotUglyBackpressure() {
        final Flux<Integer> numbers = Flux.fromIterable(List.of(1,2,3,4,5,6,7,8,9,10))
                .log();

        numbers.subscribe(new BaseSubscriber<>() {
            private int count = 0;
            private final int requestCount = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestCount);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count++;

                if (count >= requestCount) {
                    count = 0;
                    request(requestCount);
                }
            }
        });

        StepVerifier.create(numbers)
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberIntervalOne() throws InterruptedException {
        Flux<Long> interval = Flux.interval(Duration.ofMillis(100)) //imprimindo a cada 100 milisegundos, enquanto a thread estiver viva, isso vai rodar em background (em outra thread)
                .take(10) //quero apenas 10 elementos desse intervalo
                .log();

        interval.subscribe(i -> log.info("Number {}", i));

        Thread.sleep(3000); //vai parar a thread principal, assim vamos conseguir ver os valores
    }

    @Test
    public void fluxSubscriberIntervalTwo() throws InterruptedException {
        StepVerifier.withVirtualTime(this::createInterval)
                .expectSubscription()
                .expectNoEvent(Duration.ofDays(1)) //nada foi publicado antes de 1 dia
                .thenAwait(Duration.ofDays(1))
                .expectNext(0L)
                .thenAwait(Duration.ofDays(1))
                .expectNext(1L)
                .thenCancel()
                .verify();
    }

    @Test
    public void fluxSubscriberPrettBackpressure() {
        final Flux<Integer> numbers = Flux.fromIterable(List.of(1,2,3,4,5))
                .log()
                .limitRate(3); //tem que ficar após o log

        numbers.subscribe(i -> log.info("Number {}", i));

        StepVerifier.create(numbers)
                .expectNext(1,2,3,4,5)
                .verifyComplete();
    }

    @Test
    public void connectableFlux() throws InterruptedException { //publisher hot
        final ConnectableFlux<Integer> connectableFlux = Flux.range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();

        //connectableFlux.connect(); //vai começar a emitir mesmo sem um subscriber

        //log.info("Thread sleeping for 300ms");
        //Thread.sleep(300);

        //connectableFlux.subscribe(i -> log.info("Sub1 number {}", i)); //vai perder as informações que foram publicas antes do delay de 100mlis

        //Thread.sleep(200);

        //connectableFlux.subscribe(i -> log.info("Sub2 number {}", i));

        StepVerifier.create(connectableFlux)
                .then(connectableFlux::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6,7,8,9,10)
                .expectComplete()
                .verify();
    }

    @Test
    public void connectableFluxAutoConnect() { //disparar quando tiver 2 subscribers
        final Flux<Integer> fluxAutoConnect = Flux.range(1, 5)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish()
                .autoConnect(2);

        StepVerifier.create(fluxAutoConnect)
                .then(fluxAutoConnect::subscribe)
                .expectNext(1,2,3,4,5)
                .expectComplete()
                .verify();
    }

    private Flux<Long> createInterval() {
        return Flux.interval(Duration.ofDays(1))
                .take(10)
                .log();
    }
}
