package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.service.UrlShortenerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @Value("${redirect.mode:302}")
    private String redirectMode;

    @PostMapping("/api/v1/data/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(
        @Valid @RequestBody ShortenRequest request
    ) {
        String shortUrl = urlShortenerService.shortenUrl(request.longUrl());
        return ResponseEntity.ok(new ShortenResponse(shortUrl));
    }

    @GetMapping("/api/v1/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl) {
        try {
            String longUrl = urlShortenerService.getOriginalUrl(shortUrl);
            
            HttpStatus status = "301".equals(redirectMode) ? HttpStatus.MOVED_PERMANENTLY : HttpStatus.FOUND;
            
            return ResponseEntity.status(status)
                    .location(URI.create(longUrl))
                    .build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Health check endpoint for Docker
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
