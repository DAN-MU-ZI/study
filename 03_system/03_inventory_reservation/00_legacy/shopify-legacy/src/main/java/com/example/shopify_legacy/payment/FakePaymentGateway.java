package com.example.shopify_legacy.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway {

    public PaymentResult pay(Long checkoutId, String paymentToken) {
        if ("fail".equalsIgnoreCase(paymentToken)) {
            return new PaymentResult(false, null);
        }
        
        return new PaymentResult(true, "pay_" + UUID.randomUUID());
    }
}
