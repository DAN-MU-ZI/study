package com.example.idempotency.store;

import com.example.idempotency.domain.CartRecord;
import com.example.idempotency.domain.CartStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CartStore {

    private final Map<String, CartRecord> carts = new ConcurrentHashMap<>();
    private final AtomicInteger cartSequence;
    private volatile String currentCartId;

    public CartStore(
        @Value("${payment.seed-cart-id:1001}") String seedCartId,
        @Value("${payment.seed-customer-id:cust-001}") String seedCustomerId,
        @Value("${payment.seed-amount:15000}") long seedAmount
    ) {
        carts.put(seedCartId, new CartRecord(seedCartId, CartStatus.PENDING, null, null));
        currentCartId = seedCartId;
        cartSequence = new AtomicInteger(parseSequence(seedCartId));
    }

    public CartRecord get(String cartId) {
        return carts.get(cartId);
    }

    public CartRecord getCurrentCart() {
        return carts.get(currentCartId);
    }

    public synchronized CartRecord createNextCart() {
        String nextCartId = String.valueOf(cartSequence.incrementAndGet());
        CartRecord nextCart = new CartRecord(nextCartId, CartStatus.PENDING, null, null);
        carts.put(nextCartId, nextCart);
        currentCartId = nextCartId;
        return nextCart;
    }

    public void markPaid(String cartId, String paymentId, String pgTransactionId) {
        carts.computeIfPresent(cartId, (ignored, current) -> current.markPaid(paymentId, pgTransactionId));
    }

    private int parseSequence(String cartId) {
        try {
            return Integer.parseInt(cartId);
        } catch (NumberFormatException ex) {
            return 1000;
        }
    }
}
