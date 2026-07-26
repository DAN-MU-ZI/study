package com.example.dblab.query;

import java.time.LocalDateTime;

public record PostSummary(
    long id,
    String title,
    int score,
    LocalDateTime creationDate,
    Long ownerUserId,
    String tags
) {
}

