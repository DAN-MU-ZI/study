package com.example.urlshortener.service;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Codec;
import com.example.urlshortener.util.ShortCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SnowflakeGenerator idGenerator;

    private static final long TTL_SECONDS = 3600;

    public String shortenUrl(String longUrl) {
        long id = idGenerator.nextId();

        String shortUrl = Base62Codec.encode(id);

        UrlMapping mapping = new UrlMapping(id, longUrl);
        urlMappingRepository.insert(mapping);


        writeToCache(shortUrl, longUrl);

        return shortUrl;
    }

    public String getOriginalUrl(ShortCode shortCode) {
        String cacheKey = "url:" + shortCode.value();

        String cachedLongUrl = readFromCache(cacheKey);
        if (cachedLongUrl != null) {
            return cachedLongUrl;
        }

        UrlMapping mapping = urlMappingRepository.findById(shortCode.id())
                .orElseThrow(() -> new UrlNotFoundException(shortCode.value()));


        writeToCache(shortCode.value(), mapping.getLongUrl());

        return mapping.getLongUrl();
    }

    private void writeToCache(String shortUrl, String longUrl) {
        try {
            redisTemplate.opsForValue().set("url:" + shortUrl, longUrl, Duration.ofSeconds(TTL_SECONDS));
        } catch (DataAccessException e) {
            log.warn("Redis 캐시 갱신에 실패했습니다. shortUrl={}", shortUrl, e);
        }
    }

    private String readFromCache(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (DataAccessException e) {
            log.warn("Redis 캐시 조회에 실패했습니다. cacheKey={}", cacheKey, e);
            return null;
        }
    }
}
