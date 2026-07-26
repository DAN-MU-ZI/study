package com.example.idempotency.integration;

import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
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
class IdempotencyIntegrationTest {

    private static final long TASK_TIMEOUT_SECONDS = 45L;

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
    void concurrentSameKeyAndSamePayload_returnsSameApprovalAndSinglePayment() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        String customerId = uniqueCustomerId("same-key-success");
        PaymentDto.Request payload = new PaymentDto.Request(order.orderId(), customerId, 15_000L);
        String idempotencyKey = customerId + ":integration-success-" + System.nanoTime();

        executorService = Executors.newFixedThreadPool(2);
        List<PaymentDto.Response> responses = runConcurrently(
            () -> paymentService.process(idempotencyKey, payload),
            () -> paymentService.process(idempotencyKey, payload)
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).pgTransactionId()).isEqualTo(responses.get(1).pgTransactionId());
        assertThat(paymentService.getPayments(order.orderId())).hasSize(1);
    }

    @Test
    void concurrentDifferentKeysOnSameOrder_returnsSameApprovalAndSinglePayment() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        String customerId = uniqueCustomerId("different-keys-success");
        PaymentDto.Request payload = new PaymentDto.Request(order.orderId(), customerId, 15_000L);
        String firstKey = customerId + ":integration-order-a-" + System.nanoTime();
        String secondKey = customerId + ":integration-order-b-" + System.nanoTime();

        executorService = Executors.newFixedThreadPool(2);
        List<PaymentDto.Response> responses = runConcurrently(
            () -> paymentService.process(firstKey, payload),
            () -> paymentService.process(secondKey, payload)
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).pgTransactionId()).isEqualTo(responses.get(1).pgTransactionId());
        assertThat(paymentService.getPayments(order.orderId())).hasSize(1);
    }

    @Test
    void concurrentSameKeyWithDifferentPayload_rejectsWaitingRequest() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        String customerId = uniqueCustomerId("same-key-mismatch");
        PaymentDto.Request original = new PaymentDto.Request(order.orderId(), customerId, 15_000L);
        PaymentDto.Request conflicting = new PaymentDto.Request(order.orderId(), customerId, 9_900L);
        String idempotencyKey = customerId + ":integration-mismatch-" + System.nanoTime();

        executorService = Executors.newFixedThreadPool(2);
        List<Object> results = runConcurrentlyCapturingFailure(
            () -> paymentService.process(idempotencyKey, original),
            () -> paymentService.process(idempotencyKey, conflicting)
        );

        assertThat(results).anySatisfy(result -> assertThat(result).isInstanceOf(PaymentDto.Response.class));
        assertThat(results).anySatisfy(result -> {
            assertThat(result).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException exception = (ResponseStatusException) result;
            assertThat(exception.getStatusCode().value()).isEqualTo(400);
            assertThat(exception.getReason()).isEqualTo("Idempotency key used with different request payload");
        });
        assertThat(paymentService.getPayments(order.orderId())).hasSize(1);
    }

    @Test
    void concurrentDifferentKeysOnSameOrderWithDifferentPayload_rejectsWaitingRequest() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        String customerId = uniqueCustomerId("different-keys-mismatch");
        PaymentDto.Request original = new PaymentDto.Request(order.orderId(), customerId, 15_000L);
        PaymentDto.Request conflicting = new PaymentDto.Request(order.orderId(), customerId, 9_900L);
        String firstKey = customerId + ":integration-order-mismatch-a-" + System.nanoTime();
        String secondKey = customerId + ":integration-order-mismatch-b-" + System.nanoTime();

        executorService = Executors.newFixedThreadPool(2);
        List<Object> results = runConcurrentlyCapturingFailure(
            () -> paymentService.process(firstKey, original),
            () -> paymentService.process(secondKey, conflicting)
        );

        assertThat(results).anySatisfy(result -> assertThat(result).isInstanceOf(PaymentDto.Response.class));
        assertThat(results).anySatisfy(result -> {
            assertThat(result).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException exception = (ResponseStatusException) result;
            assertThat(exception.getStatusCode().value()).isEqualTo(400);
            assertThat(exception.getReason()).isEqualTo("Idempotency key used with different request payload");
        });
        assertThat(paymentService.getPayments(order.orderId())).hasSize(1);
    }

    @Test
    void concurrentSameKeyWhenLeaderFails_returnsStoredFailureToFollowers() throws Exception {
        OrderDto.Response order = paymentService.createNextOrder();
        String customerId = uniqueCustomerId("same-key-failure");
        PaymentDto.Request invalidPayload = new PaymentDto.Request(order.orderId(), customerId, 0L);
        String idempotencyKey = customerId + ":integration-failure-" + System.nanoTime();

        executorService = Executors.newFixedThreadPool(2);
        List<Object> results = runConcurrentlyCapturingFailure(
            () -> paymentService.process(idempotencyKey, invalidPayload),
            () -> paymentService.process(idempotencyKey, invalidPayload)
        );

        assertThat(results).allSatisfy(result -> {
            assertThat(result).isInstanceOf(ResponseStatusException.class);
            ResponseStatusException exception = (ResponseStatusException) result;
            assertThat(exception.getStatusCode().value()).isEqualTo(400);
            assertThat(exception.getReason()).isEqualTo("amount must be positive");
        });
        assertThat(paymentService.getPayments(order.orderId())).isEmpty();
    }

    @SafeVarargs
    private final <T> List<T> runConcurrently(Callable<T>... tasks) throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(tasks.length);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<T>> futures = Arrays.stream(tasks)
            .map(task -> executorService.submit(() -> {
                readyLatch.countDown();
                assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
                return task.call();
            }))
            .toList();

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
        return results;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private final List<Object> runConcurrentlyCapturingFailure(Callable<PaymentDto.Response>... tasks) throws Exception {
        return runConcurrently(Arrays.stream(tasks)
            .map(task -> (Callable<Object>) () -> {
                try {
                    return task.call();
                } catch (Throwable throwable) {
                    return unwrap(throwable);
                }
            })
            .toArray(Callable[]::new));
    }

    private Throwable unwrap(Throwable throwable) {
        return throwable;
    }

    private String uniqueCustomerId(String scenario) {
        return "cust-" + scenario + "-" + System.nanoTime();
    }
}
