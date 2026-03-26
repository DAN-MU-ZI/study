package com.example.idempotency.dto;

import com.example.idempotency.domain.OrderStatus;

public final class OrderDto {

    private OrderDto() {
    }

    public record Response(
        String orderId,
        OrderStatus status,
        String lastPaymentId,
        String lastPgTransactionId
    ) {
    }
}
