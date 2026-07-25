package com.example.urlshortener.dto;

public record ApiError(
    String code,
    String message
) {
}
