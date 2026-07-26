package com.example.idempotency.domain;

import java.time.Instant;

public record PaymentAttemptRecord(
    String orderId,
    String customerId,
    long amount,
    String paymentId,
    String pgTransactionId,
    OrderStatus status,
    Instant requestedAt,
    Instant approvedAt
) {
}
