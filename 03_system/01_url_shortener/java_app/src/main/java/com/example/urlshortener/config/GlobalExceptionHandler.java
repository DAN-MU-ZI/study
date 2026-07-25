package com.example.urlshortener.config;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.urlshortener.dto.ApiError;
import com.example.urlshortener.exception.UrlNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            UrlNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        "SHORT_URL_NOT_FOUND",
                        e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        "VALIDATION_ERROR",
                        "요청값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequestBody(
            HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "INVALID_REQUEST_BODY",
                        "요청 본문 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabase(
            DataAccessException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        "DATASTORE_UNAVAILABLE",
                        "저장소에 일시적으로 접근할 수 없습니다."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "INVALID_SHORT_CODE",
                        "단축 코드 형식이 올바르지 않습니다."));
    }
}
