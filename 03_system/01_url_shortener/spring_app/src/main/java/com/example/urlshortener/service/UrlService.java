package com.example.urlshortener.service;

import com.example.urlshortener.filter.RedisBloomFilter;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.zip.CRC32;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlMappingRepository repository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisBloomFilter bloomFilter;

    private static final String CACHE_PREFIX = "url:";

    public Mono<String> shortenUrl(String longUrl) {
        // 1. 이미 존재하는 URL인지 확인 (Index 활용)
        return repository.findByLongUrl(longUrl)
                .map(UrlMapping::getShortUrl)
                .switchIfEmpty(Mono.defer(() -> {
                    String shortUrlBase = generateShortUrl(longUrl, 0);
                    return checkAndGenerateUniqueShortUrl(longUrl, shortUrlBase, 0);
                }));
    }

    private String generateShortUrl(String longUrl, long salt) {
        CRC32 crc32 = new CRC32();
        crc32.update((longUrl + salt).getBytes());
        long hash = crc32.getValue();
        String hex = Long.toHexString(hash);
        
        if (hex.length() < 7) {
            return String.format("%7s", hex).replace(' ', '0');
        } else {
            return hex.substring(0, 7);
        }
    }

    private Mono<String> checkAndGenerateUniqueShortUrl(String longUrl, String shortUrl, long attempt) {
        return bloomFilter.contains(shortUrl)
                .flatMap(contains -> {
                    if (!contains) {
                        return saveUrl(shortUrl, longUrl).thenReturn(shortUrl);
                    }
                    
                    // Bloom filter says yes, check DB
                    return repository.existsByShortUrl(shortUrl)
                            .flatMap(exists -> {
                                if (exists) {
                                    // Collision: retry with salt
                                    String nextShortUrl = generateShortUrl(longUrl, System.nanoTime());
                                    return checkAndGenerateUniqueShortUrl(longUrl, nextShortUrl, attempt + 1);
                                } else {
                                    // False positive
                                    return saveUrl(shortUrl, longUrl).thenReturn(shortUrl);
                                }
                            });
                });
    }

    private Mono<Void> saveUrl(String shortUrl, String longUrl) {
        UrlMapping mapping = new UrlMapping(shortUrl, longUrl);
        return repository.save(mapping)
                .flatMap(saved -> Mono.zip(
                        bloomFilter.add(shortUrl),
                        redisTemplate.opsForValue().set(CACHE_PREFIX + shortUrl, longUrl, Duration.ofHours(24))
                ))
                .then();
    }

    public Mono<String> getLongUrl(String shortUrl) {
        // 1. Cache First
        return redisTemplate.opsForValue().get(CACHE_PREFIX + shortUrl)
                .switchIfEmpty(Mono.defer(() -> 
                    // 2. Bloom Filter Check
                    bloomFilter.contains(shortUrl)
                        .flatMap(contains -> {
                            if (!contains) return Mono.empty();
                            
                            // 3. DB Lookup
                            return repository.findByShortUrl(shortUrl)
                                    .flatMap(mapping -> 
                                        redisTemplate.opsForValue().set(CACHE_PREFIX + shortUrl, mapping.getLongUrl(), Duration.ofHours(24))
                                                .thenReturn(mapping.getLongUrl())
                                    );
                        })
                ));
    }
}
