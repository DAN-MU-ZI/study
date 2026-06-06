package com.example.shopify_legacy.payment;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final FakePaymentGateway fakePaymentGateway;

    public PaymentResult pay(Long checkoutId, String paymentToken) {
        return fakePaymentGateway.pay(checkoutId, paymentToken);
    }
}
