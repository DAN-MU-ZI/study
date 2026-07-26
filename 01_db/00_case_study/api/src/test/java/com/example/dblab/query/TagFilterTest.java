package com.example.dblab.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagFilterTest {

    @Test
    void normalize_removesAngleBracketsAndLowercasesTag() {
        assertThat(TagFilter.normalize("<Java>")).isEqualTo("java");
    }

    @Test
    void normalize_rejectsCharactersOutsideStackOverflowTagGrammar() {
        assertThatThrownBy(() -> TagFilter.normalize("java%')) OR 1=1 --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효하지 않은 태그입니다.");
    }
}

