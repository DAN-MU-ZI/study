package com.example.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global Weighted Rate Limiting Service using Redis Sorted Set (ZSET).
 * All requests share the same token bucket (for external API dependency scenario).
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    // Global key for all requests (not IP-based)
    private static final String GLOBAL_KEY = "global";

    @Value("${rate-limit.max-tokens:100}")
    private int maxTokens;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    // Token costs per operation type
    public static final Map<String, Integer> TOKEN_COSTS = Map.of(
        "read", 1,
        "write", 5,
        "delete", 10,
        "search", 3
    );

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    /**
     * Check if request is allowed using global rate limiting with weighted tokens.
     */
    public RateLimitResult checkRateLimit(int cost) {
        String key = "rate_limit:" + GLOBAL_KEY;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        zSetOps.removeRangeByScore(key, 0, windowStart);

        Set<String> members = zSetOps.rangeByScore(key, windowStart, now);
        int currentUsage = 0;
        if (members != null) {
            for (String member : members) {
                String[] parts = member.split(":");
                if (parts.length > 1) {
                    try {
                        currentUsage += Integer.parseInt(parts[parts.length - 1]);
                    } catch (NumberFormatException e) {
                        currentUsage += 1;
                    }
                }
            }
        }

        if (currentUsage + cost <= maxTokens) {
            String requestId = UUID.randomUUID().toString() + ":" + cost;
            zSetOps.add(key, requestId, now);
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1));
            return new RateLimitResult(true, maxTokens - currentUsage - cost);
        }

        return new RateLimitResult(false, Math.max(0, maxTokens - currentUsage));
    }

    /**
     * Get current global token usage statistics.
     */
    public RateLimitStats getStats() {
        String key = "rate_limit:" + GLOBAL_KEY;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        zSetOps.removeRangeByScore(key, 0, windowStart);
        
        Set<String> members = zSetOps.rangeByScore(key, windowStart, now);
        int currentUsage = 0;
        if (members != null) {
            for (String member : members) {
                String[] parts = member.split(":");
                if (parts.length > 1) {
                    try {
                        currentUsage += Integer.parseInt(parts[parts.length - 1]);
                    } catch (NumberFormatException e) {
                        currentUsage += 1;
                    }
                }
            }
        }

        return new RateLimitStats(
            "global",
            currentUsage,
            Math.max(0, maxTokens - currentUsage),
            maxTokens,
            windowSeconds,
            TOKEN_COSTS
        );
    }

    public record RateLimitResult(boolean allowed, int remaining) {}

    public record RateLimitStats(
        @com.fasterxml.jackson.annotation.JsonProperty("rate_limit_type")
        String rateLimitType,
        @com.fasterxml.jackson.annotation.JsonProperty("tokens_used")
        int tokensUsed,
        @com.fasterxml.jackson.annotation.JsonProperty("tokens_remaining")
        int tokensRemaining,
        @com.fasterxml.jackson.annotation.JsonProperty("token_limit")
        int tokenLimit,
        @com.fasterxml.jackson.annotation.JsonProperty("window_seconds")
        int windowSeconds,
        @com.fasterxml.jackson.annotation.JsonProperty("token_costs")
        Map<String, Integer> tokenCosts
    ) {}
}
