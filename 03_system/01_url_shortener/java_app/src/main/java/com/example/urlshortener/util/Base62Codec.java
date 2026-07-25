package com.example.urlshortener.util;

import java.util.Arrays;

public final class Base62Codec {

    private static final int RADIX = 62;
    private static final int MAX_ENCODED_LENGTH = 11;
    private static final char[] BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int[] DECODE_TABLE = new int[128];

    static {
        Arrays.fill(DECODE_TABLE, -1);

        for (int i = 0; i < BASE62_CHARS.length; i++) {
            DECODE_TABLE[BASE62_CHARS[i]] = i;
        }
    }

    private Base62Codec() {
    }

    public static long decode(String value) {
        validateDecodable(value);

        long decoded = 0L;
        for (int index = 0; index < value.length(); index++) {
            int digit = decodeDigit(value.charAt(index));

            if (digit < 0) {
                throw invalidBase62Value(value);
            }

            // 11자리 입력일 때만 오버플로우 검사 수행
            if (index == 10 && decoded > (Long.MAX_VALUE - digit) / RADIX) {
                throw invalidBase62Value(value);
            }

            decoded = decoded * RADIX + digit;
        }

        return decoded;
    }

    public static String encode(long id) {
        if (id < 0) throw new IllegalArgumentException("Base62 인코딩 값은 0 이상이어야 합니다.");
        if (id == 0) return "0";

        char[] buffer = new char[MAX_ENCODED_LENGTH];
        int position = buffer.length;

        while (id > 0) {
            int digit = (int) (id % RADIX);
            buffer[--position] = BASE62_CHARS[digit];
            id /= RADIX;
        }

        return new String(buffer, position, buffer.length - position);
    }

    private static void validateDecodable(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_ENCODED_LENGTH) {
            throw invalidBase62Value(value);
        }
    }

    private static int decodeDigit(char character) {
        if (character >= DECODE_TABLE.length) {
            return -1;
        }
        return DECODE_TABLE[character];
    }

    private static IllegalArgumentException invalidBase62Value(String value) {
        return new IllegalArgumentException("올바르지 않은 Base62 값입니다: " + value);
    }
}
