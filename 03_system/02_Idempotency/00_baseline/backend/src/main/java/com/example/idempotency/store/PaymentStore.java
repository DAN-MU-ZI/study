package com.example.idempotency.store;

import com.example.idempotency.domain.PaymentAttemptRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class PaymentStore {

    private final CopyOnWriteArrayList<PaymentAttemptRecord> records = new CopyOnWriteArrayList<>();

    public void add(PaymentAttemptRecord record) {
        records.add(record);
    }

    public List<PaymentAttemptRecord> findAll() {
        return records.stream()
            .sorted(Comparator.comparing(PaymentAttemptRecord::requestedAt).reversed())
            .toList();
    }

    public List<PaymentAttemptRecord> findByOrderId(String orderId) {
        return records.stream()
            .filter(record -> record.orderId().equals(orderId))
            .sorted(Comparator.comparing(PaymentAttemptRecord::requestedAt).reversed())
            .toList();
    }
}

