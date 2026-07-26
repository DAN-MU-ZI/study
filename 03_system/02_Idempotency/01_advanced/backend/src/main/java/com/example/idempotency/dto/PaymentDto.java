package com.example.idempotency.dto;

import com.example.idempotency.domain.CartStatus;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

public final class PaymentDto {

    private PaymentDto() {
    }

    public record Request(
        @JsonAlias("orderId")
        String cartId,
        String customerId,
        long amount
    ) {
    }

    public record Response(
        String cartId,
        String paymentId,
        String pgTransactionId,
        CartStatus status,
        Instant processedAt
    ) {
        public String getOrderId() {
            return cartId;
        }
    }
}
