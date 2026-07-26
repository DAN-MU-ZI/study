package com.example.idempotency.service;

import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.OrderStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

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
    void twoConcurrentRequests_onSameOrder_areCollapsedByStoreConstraint() throws Exception {
        OrderStore orderStore = new OrderStore("1001", "cust-001", 15_000L);
        PaymentStore paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(500L);
        PaymentService paymentService = new PaymentService(orderStore, paymentStore, mockPgService);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        PaymentDto.Request request = new PaymentDto.Request("1001", "cust-001", 15_000L);

        Callable<Object> task = () -> {
            readyLatch.countDown();
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start");
            }
            try {
                return paymentService.process(request);
            } catch (Throwable throwable) {
                return throwable;
            }
        };

        Future<Object> first = executorService.submit(task);
        Future<Object> second = executorService.submit(task);

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        List<Object> results = List.of(
            first.get(10, TimeUnit.SECONDS),
            second.get(10, TimeUnit.SECONDS)
        );

        assertThat(results).anySatisfy(result -> assertThat(result).isInstanceOf(PaymentDto.Response.class));
        assertThat(results).anySatisfy(result -> {
            assertThat(result).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException exception = (ResponseStatusException) result;
            assertThat(exception.getStatusCode().value()).isEqualTo(409);
            assertThat(exception.getReason()).contains("Duplicate payment attempt");
        });
        assertThat(paymentStore.findAll()).hasSize(1);
        assertThat(orderStore.get("1001").status()).isEqualTo(OrderStatus.PAID);

        executorService.shutdownNow();
    }
}
