package com.github.fabriciolfj.estudowebflux.util;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;

public class AnimeCreator {

    public static Anime createAnimeToBeSaved() {
        return Anime.builder()
                .name("Tensei")
                .build();
    }

    public static Anime createValidAnime() {
        return Anime.builder()
                .name("Tensei")
                .id(1)
                .build();
    }

    public static Anime createValidUpdatedAnime() {
        return Anime.builder()
                .name("Tensei")
                .id(1)
                .build();
    }

    public static Anime createAnimeInvalid() {
        return Anime.builder()
                .name("")
                .id(1)
                .build();
    }
}
