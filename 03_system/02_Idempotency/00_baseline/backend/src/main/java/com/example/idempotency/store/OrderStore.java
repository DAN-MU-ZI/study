package com.example.idempotency.store;

import com.example.idempotency.domain.OrderRecord;
import com.example.idempotency.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderStore {

    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderSequence;
    private volatile String currentOrderId;

    public OrderStore(
        @Value("${payment.seed-order-id:1001}") String seedOrderId,
        @Value("${payment.seed-customer-id:cust-001}") String seedCustomerId,
        @Value("${payment.seed-amount:15000}") long seedAmount
    ) {
        orders.put(seedOrderId, new OrderRecord(seedOrderId, OrderStatus.PENDING, null, null));
        currentOrderId = seedOrderId;
        orderSequence = new AtomicInteger(parseSequence(seedOrderId));
    }

    public OrderRecord get(String orderId) {
        return orders.get(orderId);
    }

    public OrderRecord getCurrentOrder() {
        return orders.get(currentOrderId);
    }

    public synchronized OrderRecord createNextOrder() {
        String nextOrderId = String.valueOf(orderSequence.incrementAndGet());
        OrderRecord nextOrder = new OrderRecord(nextOrderId, OrderStatus.PENDING, null, null);
        orders.put(nextOrderId, nextOrder);
        currentOrderId = nextOrderId;
        return nextOrder;
    }

    public void markPaid(String orderId, String paymentId, String pgTransactionId) {
        orders.computeIfPresent(orderId, (ignored, current) -> current.markPaid(paymentId, pgTransactionId));
    }

    private int parseSequence(String orderId) {
        try {
            return Integer.parseInt(orderId);
        } catch (NumberFormatException ex) {
            return 1000;
        }
    }
}

