package com.example.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.ShortCode;

@SuppressWarnings("unchecked")
class UrlShortenerServiceTest {

    @Test
    void shouldReturnCachedUrlWithoutQueryingDatabase() {
        UrlMappingRepository repository = mock(UrlMappingRepository.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:10")).thenReturn("https://example.com");

        UrlShortenerService service = new UrlShortenerService(
                repository, redisTemplate, mock(SnowflakeGenerator.class));

        assertEquals("https://example.com", service.getOriginalUrl(new ShortCode("10", 62L)));
        verify(repository, never()).findById(62L);
    }

    @Test
    void shouldCacheDatabaseResultWhenCacheMisses() {
        UrlMappingRepository repository = mock(UrlMappingRepository.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(repository.findById(62L))
                .thenReturn(Optional.of(new UrlMapping(62L, "https://example.com")));

        UrlShortenerService service = new UrlShortenerService(
                repository, redisTemplate, mock(SnowflakeGenerator.class));

        assertEquals("https://example.com", service.getOriginalUrl(new ShortCode("10", 62L)));
        verify(valueOperations).set("url:10", "https://example.com", Duration.ofHours(1));
    }

    @Test
    void shouldReadFromDatabaseWhenRedisLookupFails() {
        UrlMappingRepository repository = mock(UrlMappingRepository.class);
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SnowflakeGenerator idGenerator = mock(SnowflakeGenerator.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:10"))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(repository.findById(62L))
                .thenReturn(Optional.of(new UrlMapping(62L, "https://example.com")));

        UrlShortenerService service = new UrlShortenerService(repository, redisTemplate, idGenerator);

        String longUrl = service.getOriginalUrl(new ShortCode("10", 62L));

        assertEquals("https://example.com", longUrl);
    }
}
