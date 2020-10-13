package com.github.fabriciolfj.estudowebflux.integration;

import com.github.fabriciolfj.estudowebflux.api.exception.CustomAttributes;
import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.repository.AnimeRepository;
import com.github.fabriciolfj.estudowebflux.domain.service.AnimeService;
import com.github.fabriciolfj.estudowebflux.util.AnimeCreator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@ExtendWith(SpringExtension.class)
@WebFluxTest
@Import({AnimeService.class, CustomAttributes.class})
public class AnimeControllerIT {

    @MockBean
    private AnimeRepository animeRepositoryMock;

    @Autowired
    private WebTestClient testClient;

    private final Anime anime = AnimeCreator.createValidAnime();

    @BeforeAll
    public static void setup() {
        BlockHound.install(builder -> builder.allowBlockingCallsInside("", "")); //testar se nao estamos bloqueando alguma thread, posso passar o metodo e  a classe que quero ignorar
    }

    @BeforeEach
    public void setUp() {
        BDDMockito.when(animeRepositoryMock.findAll()).thenReturn(Flux.just(anime));
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepositoryMock.save(AnimeCreator.createAnimeToBeSaved())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepositoryMock.delete(ArgumentMatchers.any())).thenReturn(Mono.empty());
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
    @DisplayName("findAll returns a flux of anime")
    public void findAll_ReturnFluxOfAnime_WhenSuccessful() {
        testClient.get()
                .uri("/animes")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.[0].id").isEqualTo(anime.getId())
                .jsonPath("$.[0].name").isEqualTo(anime.getName());
    }

    @Test
    @DisplayName("findAll returns a flux of anime")
    public void findAll_Flavor2_ReturnFluxOfAnime_WhenSuccessful() {
        testClient.get()
                .uri("/animes")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Anime.class)
                .hasSize(1)
                .contains(anime);
    }

    @Test
    @DisplayName("findByid returns Mono with anime when it exists")
    public void findById_ReturnMonoOfAnime_WhenSuccessful() {
        testClient.get()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Anime.class)
                .isEqualTo(anime);
    }

    @Test
    @DisplayName("findByid returns Mono error does not exists")
    public void findById_ReturnMonoError_WhenEmptyIsReturned() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        testClient.get()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.developerMessage", "A ResponseStatusException Happened");
    }

    @Test
    @DisplayName("Create of anime")
    public void created_ReturnMonoOfAnime_WhenSuccessful() {
        final Anime animeSave = AnimeCreator.createAnimeToBeSaved();

        testClient.post()
                .uri("/animes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animeSave))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Anime.class)
                .isEqualTo(anime);
    }

    @Test
    @DisplayName("Create of anime, mono error bad request when name is empty")
    public void created_ReturnMonoError_WhenNameIsEmpty() {
        final Anime animeSave = AnimeCreator.createAnimeInvalid();

        testClient.post()
                .uri("/animes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animeSave))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Delete returns mono empty")
    public void delete_ReturnMono_WhenSuccessful() {
        testClient.delete()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Delete returns mono error when anime does not exists")
    public void delete_ReturnMonoErroNotExsits_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        testClient.delete()
                .uri("/animes/{id}", 3)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("update save updated anime and returns empty mono when successful")
    public void update_SAveUpdateAnime_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.save(AnimeCreator.createValidAnime())).thenReturn(Mono.empty());

        testClient.put()
                .uri("/animes/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(anime))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Update of anime, return mono empty")
    public void update_ReturnMonoEmpty_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        testClient.put()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.developerMessage", "A ResponseStatusException Happened");
    }
}
