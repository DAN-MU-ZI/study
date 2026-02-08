package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class ShortenController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public Mono<ResponseEntity<ShortenResponse>> shorten(@RequestBody ShortenRequest request) {
        return urlService.shortenUrl(request.longUrl)
                .map(shortUrl -> ResponseEntity.ok(new ShortenResponse(shortUrl)))
                .switchIfEmpty(Mono.defer(() -> {
                    ResponseEntity<ShortenResponse> badRequest = ResponseEntity.badRequest().build();
                    return Mono.just(badRequest);
                }));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShortenRequest {
        private String longUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShortenResponse {
        private String shortUrl;
    }
}
