package com.example.urlshortener.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String message) {
        super("요청한 단축 URL을 찾을 수 없습니다: " + message);
    }
}
