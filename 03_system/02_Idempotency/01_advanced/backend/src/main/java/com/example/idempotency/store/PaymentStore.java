package com.example.idempotency.store;

import com.example.idempotency.domain.PaymentAttemptRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PaymentStore {

    private final ConcurrentMap<String, PaymentAttemptRecord> recordsByOrderId = new ConcurrentHashMap<>();

    public void add(PaymentAttemptRecord record) {
        PaymentAttemptRecord existing = recordsByOrderId.putIfAbsent(record.orderId(), record);
        if (existing != null) {
            throw new DuplicatePaymentAttemptException(record.orderId());
        }
    }

    public List<PaymentAttemptRecord> findAll() {
        return recordsByOrderId.values().stream()
            .sorted(Comparator.comparing(PaymentAttemptRecord::requestedAt).reversed())
            .toList();
    }

    public List<PaymentAttemptRecord> findByOrderId(String orderId) {
        PaymentAttemptRecord record = recordsByOrderId.get(orderId);
        if (record == null) {
            return List.of();
        }
        return List.of(record);
    }

    public static class DuplicatePaymentAttemptException extends RuntimeException {

        public DuplicatePaymentAttemptException(String orderId) {
            super("Duplicate payment attempt for order: " + orderId);
        }
    }
}
