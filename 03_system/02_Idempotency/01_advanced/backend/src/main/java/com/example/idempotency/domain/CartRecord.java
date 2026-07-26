package com.example.idempotency.domain;

public record CartRecord(
    String cartId,
    CartStatus status,
    String lastPaymentId,
    String lastPgTransactionId
) {
    public CartRecord markPaid(String paymentId, String pgTransactionId) {
        return new CartRecord(cartId, CartStatus.PAID, paymentId, pgTransactionId);
    }
}
