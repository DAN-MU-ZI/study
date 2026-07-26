package com.example.idempotency.domain;

public record OrderRecord(
    String orderId,
    OrderStatus status,
    String lastPaymentId,
    String lastPgTransactionId
) {
    public OrderRecord markPaid(String paymentId, String pgTransactionId) {
        return new OrderRecord(orderId, OrderStatus.PAID, paymentId, pgTransactionId);
    }
}

