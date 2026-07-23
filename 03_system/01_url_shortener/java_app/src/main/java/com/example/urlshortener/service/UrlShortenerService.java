package com.example.urlshortener.service;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
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
    private final HashGenerator hashGenerator;
    private final RedisTemplate<String, String> redisTemplate;
    private final SnowflakeGenerator idGenerator;

    private static final long TTL_SECONDS = 3600;

    public String shortenUrl(String longUrl) {
        // Snowflake: 29bit timestamp + 2bit worker + 11bit sequence)
        long id = idGenerator.nextId();

        String shortUrl = hashGenerator.encode(id);

        UrlMapping mapping = new UrlMapping(id, longUrl);
        urlMappingRepository.insert(mapping);

        try {
            redisTemplate.opsForValue().set("url:" + shortUrl, longUrl, Duration.ofSeconds(TTL_SECONDS));
        } catch (DataAccessException e) {
            log.warn("Redis cache update failed. shortUrl={}", shortUrl, e);
        }

        return shortUrl;
    }

    public String getOriginalUrl(String shortUrl) {
        String cacheKey = "url:" + shortUrl;

        String cachedLongUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedLongUrl != null) {
            return cachedLongUrl;
        }

        long id = hashGenerator.decode(shortUrl);

        UrlMapping mapping = urlMappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        redisTemplate.opsForValue().set(cacheKey, mapping.getLongUrl(), Duration.ofSeconds(TTL_SECONDS));

        return mapping.getLongUrl();
    }
}
