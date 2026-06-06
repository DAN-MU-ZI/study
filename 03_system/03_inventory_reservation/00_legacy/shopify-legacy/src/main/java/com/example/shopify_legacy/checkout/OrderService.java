package com.example.shopify_legacy.checkout;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;

    public Order createOrder(Long checkoutId, String paymentId) {
        return orderRepository.save(Order.completed(checkoutId, paymentId));
    }
}
