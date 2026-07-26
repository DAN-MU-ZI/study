package com.example.idempotency.dto;

import com.example.idempotency.domain.CartStatus;

public final class CartDto {

    private CartDto() {
    }

    public record Response(
        String cartId,
        CartStatus status,
        String lastPaymentId,
        String lastPgTransactionId
    ) {
        public String getOrderId() {
            return cartId;
        }
    }
}
