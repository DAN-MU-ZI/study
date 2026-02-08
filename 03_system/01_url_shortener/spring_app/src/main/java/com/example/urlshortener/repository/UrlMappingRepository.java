package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UrlMappingRepository extends ReactiveCrudRepository<UrlMapping, Integer> {
    Mono<UrlMapping> findByShortUrl(String shortUrl);
    Mono<UrlMapping> findByLongUrl(String longUrl);
    Mono<Boolean> existsByShortUrl(String shortUrl);
}
