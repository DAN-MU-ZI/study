package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
    @NotBlank
    @Size(max = 2048)
    String longUrl
) {
}
