package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @Value("${redirect.mode:302}")
    private String redirectMode;

    @PostMapping("/api/v1/data/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request) {
        String shortUrl = urlShortenerService.shortenUrl(request.getLongUrl());
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

    @Data
    public static class ShortenRequest {
        private String longUrl;
    }

    @Data
    public static class ShortenResponse {
        private String shortUrl;

        public ShortenResponse(String shortUrl) {
            this.shortUrl = shortUrl;
        }
    }
}
