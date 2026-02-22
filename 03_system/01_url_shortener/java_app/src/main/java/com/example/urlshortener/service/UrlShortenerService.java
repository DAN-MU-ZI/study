package com.example.urlshortener.service;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final HashGenerator hashGenerator;
    private final RedisTemplate<String, String> redisTemplate;
    private final SequenceGeneratorService sequenceGeneratorService;

    private static final long TTL_SECONDS = 3600;

    public String shortenUrl(String longUrl) {
        // 1. Generate unique ID
        long id = sequenceGeneratorService.generateSequence("url_sequence");

        // 2. Encode ID to Base62 (shortUrl)
        String shortUrl = hashGenerator.encode(id);

        // 3. Save to DB (using Long ID)
        UrlMapping mapping = new UrlMapping(id, longUrl);
        urlMappingRepository.save(mapping);

        // 4. Cache Update (Key: "url:shortUrl")
        redisTemplate.opsForValue().set("url:" + shortUrl, longUrl, Duration.ofSeconds(TTL_SECONDS));

        return shortUrl;
    }

    public String getOriginalUrl(String shortUrl) {
        String cacheKey = "url:" + shortUrl;

        // Look Aside: Cache Check
        String cachedLongUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedLongUrl != null) {
            return cachedLongUrl;
        }

        // Cache Miss: DB Check
        // 1. Decode shortUrl to ID
        long id = hashGenerator.decode(shortUrl);

        // 2. Find by ID
        UrlMapping mapping = urlMappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Cache Update
        redisTemplate.opsForValue().set(cacheKey, mapping.getLongUrl(), Duration.ofSeconds(TTL_SECONDS));

        return mapping.getLongUrl();
    }
}
