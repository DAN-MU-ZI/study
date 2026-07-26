package com.example.idempotency.domain;

import java.time.Instant;

public record PaymentAttemptRecord(
    String cartId,
    String customerId,
    long amount,
    String paymentId,
    String pgTransactionId,
    CartStatus status,
    Instant requestedAt,
    Instant approvedAt
) {
    public String getOrderId() {
        return cartId;
    }
}
