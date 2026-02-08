package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    @Value("${app.redirect-mode:302}")
    private String redirectMode;

    @GetMapping("/{shortUrl}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortUrl) {
        return urlService.getLongUrl(shortUrl)
                .map(longUrl -> {
                    HttpStatus status = "301".equals(redirectMode) ? 
                            HttpStatus.MOVED_PERMANENTLY : HttpStatus.FOUND;
                    ResponseEntity<Void> response = ResponseEntity.status(status)
                            .location(URI.create(longUrl))
                            .build();
                    return response;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    ResponseEntity<Void> response = ResponseEntity.notFound().build();
                    return Mono.just(response);
                }));
    }
}
