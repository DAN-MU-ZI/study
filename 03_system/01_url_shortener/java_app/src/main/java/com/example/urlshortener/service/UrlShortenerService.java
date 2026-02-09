package com.example.urlshortener.service;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final HashGenerator hashGenerator;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_RETRIES = 5;
    private static final long TTL_SECONDS = 3600;

    @Transactional
    public String shortenUrl(String longUrl) {
        String salt = "";
        
        for (int i = 0; i < MAX_RETRIES; i++) {
            if (i > 0) {
                salt = generateRandomSalt();
            }

            String candidateShort = hashGenerator.generateShortUrl(longUrl + salt, 7);

            try {
                // Write-Through pattern: Save to DB first
                 if (urlMappingRepository.existsById(candidateShort)) {
                    log.warn("Collision detected for shortUrl: {}", candidateShort);
                    continue;
                }
                
                UrlMapping mapping = new UrlMapping(candidateShort, longUrl);
                urlMappingRepository.save(mapping);

                // Cache Update
                String cacheKey = "url:" + candidateShort;
                redisTemplate.opsForValue().set(cacheKey, longUrl, Duration.ofSeconds(TTL_SECONDS));

                return candidateShort;

            } catch (Exception e) {
                log.error("Error saving URL mapping", e);
                // Retry with new salt
            }
        }
        
        throw new RuntimeException("Failed to generate unique short URL after " + MAX_RETRIES + " attempts");
    }

    public String getOriginalUrl(String shortUrl) {
        String cacheKey = "url:" + shortUrl;

        // Look Aside: Cache Check
        String cachedLongUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedLongUrl != null) {
            return cachedLongUrl;
        }

        // Cache Miss: DB Check
        UrlMapping mapping = urlMappingRepository.findById(shortUrl)
                .orElseThrow(() -> new RuntimeException("Short URL not found"));

        // Cache Update
        redisTemplate.opsForValue().set(cacheKey, mapping.getLongUrl(), Duration.ofSeconds(TTL_SECONDS));

        return mapping.getLongUrl();
    }

    private String generateRandomSalt() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 4; i++) {
            salt.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return salt.toString();
    }
}
