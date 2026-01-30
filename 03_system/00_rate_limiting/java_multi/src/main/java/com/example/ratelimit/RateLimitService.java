package com.example.ratelimit;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global Weighted Rate Limiting Service using Lua Script for Atomic Operations.
 * All requests share the same token bucket (for external API dependency scenario).
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> rateLimitScript;

    // Global key for all requests (not IP-based)
    private static final String GLOBAL_KEY = "global";

    @Value("${rate-limit.max-tokens:100}")
    private int maxTokens;

    @Value("${rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${rate-limit.server-id:unknown}")
    private String serverId;

    // Token costs per operation type
    public static final Map<String, Integer> TOKEN_COSTS = Map.of(
        "read", 1,
        "write", 5,
        "delete", 10,
        "search", 3
    );

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(
            new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua"))
        );
        rateLimitScript.setResultType(Long.class);
    }

    /**
     * Check rate limit using Lua script for atomicity with global weighted tokens.
     */
    public RateLimitResult checkRateLimitAtomic(int cost) {
        String key = "rate_limit:" + GLOBAL_KEY;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);
        String requestId = UUID.randomUUID().toString() + ":" + cost;

        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(key),
            String.valueOf(windowStart),
            String.valueOf(now),
            requestId,
            String.valueOf(maxTokens),
            String.valueOf(windowSeconds),
            String.valueOf(cost)
        );

        if (result != null && result >= 0) {
            return new RateLimitResult(true, result.intValue(), serverId);
        }

        return new RateLimitResult(false, 0, serverId);
    }

    /**
     * Get current global token usage statistics.
     */
    public RateLimitStats getStats() {
        String key = "rate_limit:" + GLOBAL_KEY;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(key, windowStart, now);
        
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
            serverId,
            TOKEN_COSTS
        );
    }

    public String getServerId() {
        return serverId;
    }


    public record RateLimitResult(boolean allowed, int remaining, @com.fasterxml.jackson.annotation.JsonProperty("server_id") String serverId) {}

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
        @com.fasterxml.jackson.annotation.JsonProperty("server_id")
        String serverId,
        @com.fasterxml.jackson.annotation.JsonProperty("token_costs")
        Map<String, Integer> tokenCosts
    ) {}
}
