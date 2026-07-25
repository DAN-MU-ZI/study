package com.example.urlshortener.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ShortCodeConverterTest {

    private final ShortCodeConverter converter = new ShortCodeConverter();

    @Test
    void shouldConvertCanonicalBase62Value() {
        ShortCode shortCode = converter.convert("10");

        assertEquals("10", shortCode.value());
        assertEquals(62L, shortCode.id());
    }

    @Test
    void shouldRejectZeroId() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("0"));
    }

    @Test
    void shouldRejectLeadingZero() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("01"));
    }
}
