package com.example.urlshortener.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShortenRequest(
    @NotBlank(message = "원본 URL은 필수입니다.")
    @Size(max = 2048, message = "원본 URL은 2,048자 이하여야 합니다.")
    @URL(message = "올바른 URL 형식이 아닙니다.")
    String longUrl
) {
}
