package com.github.fabriciolfj.estudowebflux.domain.service;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.repository.AnimeRepository;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

@ExtendWith(SpringExtension.class)
class AnimeServiceTest {

    @InjectMocks
    private AnimeService animeService;

    @Mock
    private AnimeRepository animeRepository;

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
        BDDMockito.when(animeRepository.findAll()).thenReturn(Flux.just(anime));
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepository.save(AnimeCreator.createAnimeToBeSaved())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepository.delete(ArgumentMatchers.any(Anime.class)))
                .thenReturn(Mono.empty());

        BDDMockito.when(animeRepository.save(AnimeCreator.createValidAnime())).thenReturn(Mono.just(anime));
    }

    @Test
    @DisplayName("findAll returns a flux of anime")
    public void findAll_ReturnFluxOfAnime_WhenSuccessful() {
        StepVerifier.create(animeService.findAll())
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }


    @Test
    @DisplayName("findByid returns Mono with anime when it exists")
    public void findById_ReturnMonoOfAnime_WhenSuccessful() {
        StepVerifier.create(animeService.findById(1))
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }

    @Test
    @DisplayName("findByid returns Mono error does not exists")
    public void findById_ReturnMonoError_WhenEmptyIsReturned() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(animeService.findById(1))
                .expectSubscription()
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    @DisplayName("Create of anime")
    public void created_ReturnMonoOfAnime_WhenSuccessful() {
        StepVerifier.create(animeService.save(AnimeCreator.createAnimeToBeSaved()))
                .expectSubscription()
                .expectNext(anime)
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete of anime")
    public void delete_ReturnMonoEmtpty_WhenSuccessful() {
        StepVerifier.create(animeService.delete(1))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete returns mono error when anime does not exists")
    public void delete_ReturnMonoErroNotExsits_WhenSuccessful() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(animeService.delete(1))
                .expectSubscription()
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    @DisplayName("Update of anime, return mono empty")
    public void update_ReturnMonoEmpty_WhenSuccessful() {
        StepVerifier.create(animeService.update(1, AnimeCreator.createValidAnime()))
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    @DisplayName("Update of anime, return mono error does not exists anime")
    public void update_ReturnMonoError_WhenSuccessful() {
        BDDMockito.when(animeRepository.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(animeService.update(1, AnimeCreator.createValidAnime()))
                .expectSubscription()
                .expectError()
                .verify();
    }


}