package com.example.idempotency.integration;

import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    properties = {
        "payment.processing-delay-ms=250",
        "payment.pg-delay-ms=200"
    }
)
class BaselineIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    private ExecutorService executorService;

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentRequests_onSameOrder_createTwoApprovals() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        PaymentDto.Request payload = new PaymentDto.Request(order.orderId(), "cust-001", 15_000L);

        executorService = Executors.newFixedThreadPool(2);
        List<PaymentDto.Response> responses = runConcurrently(
            () -> paymentService.process(payload),
            () -> paymentService.process(payload)
        );

        assertThat(responses).hasSize(2);
        assertThat(new HashSet<>(responses.stream().map(PaymentDto.Response::pgTransactionId).toList())).hasSize(2);
        assertThat(paymentService.getPayments(order.orderId())).hasSize(2);
        assertThat(paymentService.getOrder(order.orderId()).status()).isEqualTo("PAID");
    }

    @SafeVarargs
    private final <T> List<T> runConcurrently(Callable<T>... tasks) throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(tasks.length);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
                return task.call();
            }));
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }
        return results;
    }
}
