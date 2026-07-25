package com.example.urlshortener.util;

import java.util.Objects;

public record ShortCode (
    String value,
    long id
) {

    public ShortCode {
        Objects.requireNonNull(
                value,
                "단축 코드는 null일 수 없습니다."
        );

        if (value.isEmpty()) {
            throw new IllegalArgumentException("단축 코드는 비어 있을 수 없습니다.");
        }

        if (id <= 0) {
            throw new IllegalArgumentException("단축 코드 ID는 양수여야 합니다.");
        }

        if (value.length() > 1 && value.charAt(0) == '0') {
            throw new IllegalArgumentException(
                    "단축 코드에 선행 0을 사용할 수 없습니다."
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
