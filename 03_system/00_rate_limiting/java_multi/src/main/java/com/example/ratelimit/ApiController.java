package com.example.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API Controller with Global Weighted Rate Limiting.
 * All requests share the same token bucket.
 */
@RestController
public class ApiController {

    private final RateLimitService rateLimitService;

    public ApiController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "rate-limit-api-global-weighted-java",
            "server_id", rateLimitService.getServerId(),
            "rate_limit_type", "global",
            "token_costs", RateLimitService.TOKEN_COSTS
        ));
    }

    @GetMapping("/api/resource")
    public ResponseEntity<Map<String, Object>> getResource() {
        int cost = RateLimitService.TOKEN_COSTS.get("read");
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimitAtomic(cost);

        if (!result.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Token limit exceeded. You need " + cost + " tokens.",
                    "tokens_remaining", result.remaining(),
                    "server_id", result.serverId()
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "GET request successful",
            "operation", "read",
            "tokens_consumed", cost,
            "tokens_remaining", result.remaining(),
            "server_id", result.serverId()
        ));
    }

    @PostMapping("/api/resource")
    public ResponseEntity<Map<String, Object>> createResource() {
        int cost = RateLimitService.TOKEN_COSTS.get("write");
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimitAtomic(cost);

        if (!result.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Token limit exceeded. You need " + cost + " tokens.",
                    "tokensRemaining", result.remaining()
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "POST request successful",
            "operation", "write",
            "tokens_consumed", cost,
            "tokens_remaining", result.remaining(),
            "server_id", result.serverId()
        ));
    }

    @DeleteMapping("/api/resource/{resourceId}")
    public ResponseEntity<Map<String, Object>> deleteResource(@PathVariable String resourceId) {
        int cost = RateLimitService.TOKEN_COSTS.get("delete");
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimitAtomic(cost);

        if (!result.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Token limit exceeded. You need " + cost + " tokens.",
                    "tokensRemaining", result.remaining()
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "DELETE request successful for resource " + resourceId,
            "operation", "delete",
            "tokens_consumed", cost,
            "tokens_remaining", result.remaining(),
            "server_id", result.serverId()
        ));
    }

    @GetMapping("/api/search")
    public ResponseEntity<Map<String, Object>> searchResources(@RequestParam(defaultValue = "") String q) {
        int cost = RateLimitService.TOKEN_COSTS.get("search");
        RateLimitService.RateLimitResult result = rateLimitService.checkRateLimitAtomic(cost);

        if (!result.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "error", "Too Many Requests",
                    "message", "Token limit exceeded. You need " + cost + " tokens.",
                    "tokensRemaining", result.remaining()
                ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Search completed",
            "operation", "search",
            "query", q,
            "tokens_consumed", cost,
            "tokens_remaining", result.remaining(),
            "server_id", result.serverId()
        ));
    }

    @GetMapping("/api/stats")
    public ResponseEntity<RateLimitService.RateLimitStats> getStats() {
        return ResponseEntity.ok(rateLimitService.getStats());
    }
}
