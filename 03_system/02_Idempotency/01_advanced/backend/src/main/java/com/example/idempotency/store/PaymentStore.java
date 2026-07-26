package com.example.idempotency.store;

import com.example.idempotency.domain.PaymentAttemptRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PaymentStore {

    private final ConcurrentMap<String, PaymentAttemptRecord> recordsByCartId = new ConcurrentHashMap<>();

    public void add(PaymentAttemptRecord record) {
        PaymentAttemptRecord existing = recordsByCartId.putIfAbsent(record.cartId(), record);
        if (existing != null) {
            throw new DuplicatePaymentAttemptException(record.cartId());
        }
    }

    public List<PaymentAttemptRecord> findAll() {
        return recordsByCartId.values().stream()
            .sorted(Comparator.comparing(PaymentAttemptRecord::requestedAt).reversed())
            .toList();
    }

    public List<PaymentAttemptRecord> findByCartId(String cartId) {
        PaymentAttemptRecord record = recordsByCartId.get(cartId);
        if (record == null) {
            return List.of();
        }
        return List.of(record);
    }

    public static class DuplicatePaymentAttemptException extends RuntimeException {

        public DuplicatePaymentAttemptException(String cartId) {
            super("Duplicate payment attempt for cart: " + cartId);
        }
    }
}
