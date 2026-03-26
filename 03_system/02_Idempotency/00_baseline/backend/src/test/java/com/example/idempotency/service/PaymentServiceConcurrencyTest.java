package com.example.idempotency.service;

import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.OrderStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceConcurrencyTest {

    @Test
    void twoConcurrentRequests_onSameOrder_createTwoPgApprovals() throws Exception {
        OrderStore orderStore = new OrderStore("1001", "cust-001", 15_000L);
        PaymentStore paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(500L);
        PaymentService paymentService = new PaymentService(orderStore, paymentStore, mockPgService, 700L);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        PaymentDto.Request request = new PaymentDto.Request("1001", "cust-001", 15_000L);

        Callable<?> task = () -> {
            readyLatch.countDown();
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start");
            }
            return paymentService.process(request);
        };

        Future<?> first = executorService.submit(task);
        Future<?> second = executorService.submit(task);

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);

        List<String> pgTransactionIds = paymentStore.findAll().stream()
            .map(record -> record.pgTransactionId())
            .toList();

        assertThat(paymentStore.findAll()).hasSize(2);
        assertThat(new HashSet<>(pgTransactionIds)).hasSize(2);
        assertThat(orderStore.get("1001").status().name()).isEqualTo("PAID");

        executorService.shutdownNow();
    }
}

