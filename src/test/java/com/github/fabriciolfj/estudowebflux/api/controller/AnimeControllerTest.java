package com.github.fabriciolfj.estudowebflux.api.controller;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.service.AnimeService;
import com.github.fabriciolfj.estudowebflux.util.AnimeCreator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@ExtendWith(SpringExtension.class)
class AnimeControllerTest {

    @InjectMocks
    private AnimeController animeController;

    @Mock
    private AnimeService animeService;

    private final Anime anime = AnimeCreator.createValidAnime();

    @BeforeAll
    public static void setup() {
        BlockHound.install(builder -> builder.allowBlockingCallsInside("", "")); //testar se nao estamos bloqueando alguma thread, posso passar o metodo e  a classe que quero ignorar
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

    @BeforeEach
    public void setUp() {
        BDDMockito.when(animeService.findAll()).thenReturn(Flux.just(anime));
        BDDMockito.when(animeService.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeService.save(AnimeCreator.createAnimeToBeSaved())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeService.delete(ArgumentMatchers.anyInt()))
                .thenReturn(Mono.empty());

        BDDMockito.when(animeService.save(AnimeCreator.createValidAnime())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeService.update(1, AnimeCreator.createValidAnime())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("findAll returns a flux of anime")
    public void findAll_ReturnFluxOfAnime_WhenSuccessful() {
        StepVerifier.create(animeController.listAll())
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }

    @Test
    @DisplayName("findByid returns Mono with anime when it exists")
    public void findById_ReturnMonoOfAnime_WhenSuccessful() {
        StepVerifier.create(animeController.findById(1))
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }

    @Test
    @DisplayName("findByid returns Mono error does not exists")
    public void findById_ReturnMonoError_WhenEmptyIsReturned() {
        BDDMockito.when(animeService.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(animeController.findById(1))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    @DisplayName("Create of anime")
    public void created_ReturnMonoOfAnime_WhenSuccessful() {
        StepVerifier.create(animeController.save(AnimeCreator.createAnimeToBeSaved()))
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete of anime")
    public void delete_ReturnMonoEmtpty_WhenSuccessful() {
        StepVerifier.create(animeController.delete(1))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete returns mono error when anime does not exists")
    public void delete_ReturnMonoErroNotExsits_WhenSuccessful() {
        BDDMockito.when(animeService.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(animeController.delete(1))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    @DisplayName("Update of anime, return mono empty")
    public void update_ReturnMonoEmpty_WhenSuccessful() {
        StepVerifier.create(animeController.update(1, AnimeCreator.createValidAnime()))
                .expectSubscription()
                .verifyComplete();
    }

}