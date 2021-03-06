package com.github.fabriciolfj.estudowebflux.api.controller;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import com.github.fabriciolfj.estudowebflux.domain.service.AnimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/animes")
@Slf4j
@SecurityScheme(
        name = "Basic Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class AnimeController {

    private final AnimeService animeService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List all animes", security = @SecurityRequirement(name = "Basic Authentication"), tags = {"anime"})
    public Flux<Anime> listAll() {
        return animeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Anime> findById(@PathVariable("id") final Integer id) {
        return animeService.findById(id);
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Anime> save(@RequestBody @Valid final Anime anime) {
        return animeService.save(anime);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<Anime> saveBatch(@RequestBody final List<Anime> animes) {
        return animeService.saveBatch(animes);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> update(@PathVariable("id") final int id, @RequestBody @Valid final Anime anime) {
        return animeService.update(id, anime);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable("id") final int id) {
        return animeService.delete(id);
    }
}
