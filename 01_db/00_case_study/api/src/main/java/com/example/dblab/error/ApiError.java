package com.example.dblab.error;

public record ApiError(ErrorBody error) {

    public static ApiError validation(String message) {
        return new ApiError(new ErrorBody("VALIDATION_ERROR", message));
    }

    public record ErrorBody(String code, String message) {
    }
}

