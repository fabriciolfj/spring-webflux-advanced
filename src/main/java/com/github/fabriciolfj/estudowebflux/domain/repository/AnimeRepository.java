package com.github.fabriciolfj.estudowebflux.domain.repository;

import com.github.fabriciolfj.estudowebflux.domain.entity.Anime;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AnimeRepository extends ReactiveCrudRepository<Anime, Integer> {

}
