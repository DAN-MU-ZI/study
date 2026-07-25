package com.example.urlshortener.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class Base62CodecTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1, 1",
            "61, z",
            "62, 10",
            "63, 11"
    })
    void shouldEncodeBoundaryValues(long id, String expected) {
        assertEquals(expected, Base62Codec.encode(id));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 61L, 62L, 63L, Long.MAX_VALUE})
    void shouldRestoreOriginalIdAfterRoundTrip(long id) {
        assertEquals(id, Base62Codec.decode(Base62Codec.encode(id)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!", "abc-", "가"})
    void shouldRejectCharactersOutsideBase62Alphabet(String value) {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.decode(value));
    }

    @Test
    void shouldRejectEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.decode(""));
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.decode(null));
    }

    @Test
    void shouldRejectValueLongerThanElevenCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.decode("000000000000"));
    }

    @Test
    void shouldRejectValueGreaterThanLongMaxValue() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.decode("AzL8n0Y58m8"));
    }

    @Test
    void shouldRejectNegativeId() {
        assertThrows(IllegalArgumentException.class, () -> Base62Codec.encode(-1L));
    }
}
