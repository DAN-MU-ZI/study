package com.example.idempotency.dto;

public final class OrderDto {

    private OrderDto() {
    }

    public record Response(
        String orderId,
        String status,
        String lastPaymentId,
        String lastPgTransactionId
    ) {
    }
}

