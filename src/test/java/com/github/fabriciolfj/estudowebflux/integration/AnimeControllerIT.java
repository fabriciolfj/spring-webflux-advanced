package com.github.fabriciolfj.estudowebflux.integration;

import com.github.fabriciolfj.estudowebflux.api.exception.CustomAttributes;
import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.repository.AnimeRepository;
import com.github.fabriciolfj.estudowebflux.domain.service.AnimeService;
import com.github.fabriciolfj.estudowebflux.util.AnimeCreator;
import com.github.fabriciolfj.estudowebflux.util.WebTestClientUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureWebTestClient
@Import({AnimeService.class, CustomAttributes.class})
public class AnimeControllerIT {
    
    private final static String USER = "lucas";
    private final static String ADMIN = "fabricio";
    private final static String INVALID = "invalid";

    @Autowired
    private WebTestClientUtil util;
    
    @Autowired
    private WebTestClient client;

    @MockBean
    private AnimeRepository animeRepositoryMock;

    /*private WebTestClient client;
    private WebTestClient client;
    private WebTestClient testClientInvalid;*/

    private final Anime anime = AnimeCreator.createValidAnime();
    private final List<Anime> animes = List.of(anime, anime);

    @BeforeAll
    public static void setup() {
        BlockHound.install(builder -> builder.allowBlockingCallsInside("java.util.UUID", "randomUUID")); //testar se nao estamos bloqueando alguma thread, posso passar o metodo e  a classe que quero ignorar
    }

    @BeforeEach
    public void setUp() {
        /*client = util.authenticateClient("lucas", "1234");
        client = util.authenticateClient("fabricio", "1234");
        testClientInvalid = util.authenticateClient("invalid", "1234");*/
        BDDMockito.when(animeRepositoryMock.findAll()).thenReturn(Flux.just(anime));
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepositoryMock.save(AnimeCreator.createAnimeToBeSaved())).thenReturn(Mono.just(anime));
        BDDMockito.when(animeRepositoryMock.delete(ArgumentMatchers.any())).thenReturn(Mono.empty());
        BDDMockito.when(animeRepositoryMock.saveAll(animes)).thenReturn(Flux.just(anime, anime));
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
    @WithUserDetails(USER)
    public void findAll_ReturnFluxOfAnime_WhenSuccessful() {
        client.get()
                .uri("/animes")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.[0].id").isEqualTo(anime.getId())
                .jsonPath("$.[0].name").isEqualTo(anime.getName());
    }

    @Test
    @DisplayName("findAll returns a flux of anime")
    @WithUserDetails(USER)
    public void findAll_Flavor2_ReturnFluxOfAnime_WhenSuccessful() {
        client.get()
                .uri("/animes")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Anime.class)
                .hasSize(1)
                .contains(anime);
    }

    @Test
    @DisplayName("findByid returns Mono with anime when it exists")
    @WithUserDetails(USER)
    public void findById_ReturnMonoOfAnime_WhenSuccessful() {
        client.get()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Anime.class)
                .isEqualTo(anime);
    }

    @Test
    @DisplayName("findByid returns Mono error does not exists")
    @WithUserDetails(USER)
    public void findById_ReturnMonoError_WhenEmptyIsReturned() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        client.get()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.developerMessage", "A ResponseStatusException Happened");
    }

    @Test
    @DisplayName("Create of anime")
    @WithUserDetails(ADMIN)
    public void created_ReturnMonoOfAnime_WhenSuccessful() {
        final Anime animeSave = AnimeCreator.createAnimeToBeSaved();

        client.post()
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
    @WithUserDetails(ADMIN)
    public void created_ReturnMonoError_WhenNameIsEmpty() {
        final Anime animeSave = AnimeCreator.createAnimeInvalid();

        client.post()
                .uri("/animes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animeSave))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @DisplayName("Delete returns mono empty")
    @WithUserDetails(ADMIN)
    public void delete_ReturnMono_WhenSuccessful() {
        client.delete()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Delete returns mono error when anime does not exists")
    @WithUserDetails(ADMIN)
    public void delete_ReturnMonoErroNotExsits_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        client.delete()
                .uri("/animes/{id}", 3)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("update save updated anime and returns empty mono when successful")
    @WithUserDetails(ADMIN)
    public void update_SAveUpdateAnime_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.save(AnimeCreator.createValidAnime())).thenReturn(Mono.empty());

        client.put()
                .uri("/animes/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(anime))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Update of anime, return mono empty")
    @WithUserDetails(ADMIN)
    public void update_ReturnMonoEmpty_WhenSuccessful() {
        BDDMockito.when(animeRepositoryMock.findById(ArgumentMatchers.anyInt())).thenReturn(Mono.empty());

        client.put()
                .uri("/animes/{id}", 1)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.developerMessage", "A ResponseStatusException Happened");
    }

    @Test
    @DisplayName("Create batch animes")
    @WithUserDetails(ADMIN)
    public void created_ReturnFluxOfAnime_WhenSuccessful() {

        client.post()
                .uri("/animes/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(animes))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(Anime.class)
                .hasSize(2)
                .contains(anime);
    }

    @Test
    @DisplayName("saveAll returns mono error when one of the objects list contains name is empty")
    @WithUserDetails(ADMIN)
    public void created_ReturnMonoErrorSaveAll_WhenSuccessful() {
        final Anime anime = AnimeCreator.createValidAnime();
        final Anime animeNotName = AnimeCreator.createAnimeNotName();

        BDDMockito.when(animeRepositoryMock.saveAll(ArgumentMatchers.anyIterable())).thenReturn(Flux.just(anime, animeNotName));

        client.post()
                .uri("/animes/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(List.of(anime, animeNotName)))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    @DisplayName("role admin not permissions findall")
    @WithUserDetails(ADMIN)
    public void findAll_Forbidden() {
        client.get()
                .uri("/animes")
                .exchange()
                .expectStatus().isForbidden();
    }

    /*@Test
    @DisplayName("role admin not authenticated findall")
    @WithUserDetails(INVALID)
    public void findAll_NotAuthenticated() {
        client.get()
                .uri("/animes")
                .exchange()
                .expectStatus().isUnauthorized();
    }*/
}
