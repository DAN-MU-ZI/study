package com.example.urlshortener.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UrlNotFoundExceptionTest {

    @Test
    void shouldExposeKoreanMessage() {
        UrlNotFoundException exception = new UrlNotFoundException("abc123");

        assertEquals("요청한 단축 URL을 찾을 수 없습니다: abc123", exception.getMessage());
    }
}
