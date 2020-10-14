package com.github.fabriciolfj.estudowebflux.domain.repository;

import com.github.fabriciolfj.estudowebflux.domain.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Integer> {

    Mono<User> findByUsername(final String username);
}
