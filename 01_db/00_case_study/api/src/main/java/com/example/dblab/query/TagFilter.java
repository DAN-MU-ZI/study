package com.example.dblab.query;

import java.util.Locale;
import java.util.regex.Pattern;

final class TagFilter {

    private static final Pattern VALID_TAG = Pattern.compile("[a-z0-9][a-z0-9+#.-]{0,34}");

    private TagFilter() {
    }

    static String normalize(String rawTag) {
        if (rawTag == null) {
            throw new IllegalArgumentException("유효하지 않은 태그입니다.");
        }

        var normalized = rawTag.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("<") && normalized.endsWith(">") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (!VALID_TAG.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효하지 않은 태그입니다.");
        }
        return normalized;
    }
}

