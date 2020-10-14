package com.github.fabriciolfj.estudowebflux.domain.service;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.repository.AnimeRepository;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;

    public Flux<Anime> findAll() {
        return animeRepository.findAll();
    }

    public Mono<Anime> findById(final Integer id) {
        return animeRepository.findById(id)
                .switchIfEmpty(monoResponseStatusNotFoundException())
                .log();
    }

    public <T> Mono<T> monoResponseStatusNotFoundException() {
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime not found"));
    }

    @Transactional
    public Flux<Anime> saveBatch(final List<Anime> animes) {
        return animeRepository.saveAll(animes)
                .doOnNext(e -> {
                    log.info("Anime fail: {}", e);
                    throwResponseStatusExceptionWhenEmptyName(e);
                }).log();
    }

    private void throwResponseStatusExceptionWhenEmptyName(final Anime anime) {
        if(StringUtil.isNullOrEmpty(anime.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid name");
        }
    }

    public Mono<Anime> save(final Anime anime) {
        return animeRepository.save(anime)
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fail save anime. Details: " +e.getMessage()));
    }

    public Mono<Void> update(final int id, final Anime anime) {
        return findById(id)
                .flatMap(a -> {
                    a.setName(anime.getName());
                    return animeRepository.save(a);
                }).then();
    }

    public Mono<Void> delete(int id) {
        return findById(id)
                .flatMap(animeRepository::delete);
    }
}
