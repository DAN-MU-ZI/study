package com.example.urlshortener.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    // Redisson starter provides RedissonClient, 
    // and spring-data-redis provides StringRedisTemplate automatically.
    // No manual beans needed for now.
}
