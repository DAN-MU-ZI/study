package com.example.idempotency.dto;

import java.time.Instant;

public final class PaymentDto {

    private PaymentDto() {
    }

    public record Request(
        String orderId,
        String customerId,
        long amount
    ) {
    }

    public record Response(
        String orderId,
        String paymentId,
        String pgTransactionId,
        String status,
        Instant processedAt,
        String threadName
    ) {
    }
}

