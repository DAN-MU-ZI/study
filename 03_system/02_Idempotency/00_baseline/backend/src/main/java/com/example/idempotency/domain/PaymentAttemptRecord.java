package com.example.idempotency.domain;

import java.time.Instant;

public record PaymentAttemptRecord(
    String orderId,
    String customerId,
    long amount,
    String paymentId,
    String pgTransactionId,
    String status,
    String threadName,
    Instant requestedAt,
    Instant approvedAt
) {
}

