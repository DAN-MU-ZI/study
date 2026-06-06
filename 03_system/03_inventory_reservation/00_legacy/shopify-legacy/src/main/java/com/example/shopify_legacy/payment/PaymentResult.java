package com.example.shopify_legacy.payment;

public record PaymentResult(
    boolean succeeded,
    String paymentId
) {
}
