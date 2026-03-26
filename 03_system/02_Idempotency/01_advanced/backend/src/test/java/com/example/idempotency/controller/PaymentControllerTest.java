package com.example.idempotency.controller;

import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.MockPgService;
import com.example.idempotency.service.PaymentService;
import com.example.idempotency.store.OrderStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentControllerTest {

    private HealthController healthController;
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        OrderStore orderStore = new OrderStore("1001", "cust-001", 15_000L);
        PaymentStore paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(0L);
        PaymentService paymentService = new PaymentService(orderStore, paymentStore, mockPgService);

        healthController = new HealthController();
        paymentController = new PaymentController(paymentService);
    }

    @Test
    void healthEndpoint_returnsHealthy() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "healthy");
        assertThat(response.getBody()).containsEntry("service", "idempotency-advanced");
    }

    @Test
    void postPayments_returnsPaymentResponseShape() {
        ResponseEntity<PaymentDto.Response> response = paymentController.process(
            null,
            new PaymentDto.Request("1001", "cust-001", 15_000L)
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isEqualTo("1001");
        assertThat(response.getBody().paymentId()).startsWith("pay-");
        assertThat(response.getBody().pgTransactionId()).startsWith("pg-");
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void getPayments_returnsEvidenceList() {
        paymentController.process(null, new PaymentDto.Request("1001", "cust-001", 15_000L));

        ResponseEntity<List<PaymentAttemptRecord>> response = paymentController.getPayments("1001");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        PaymentAttemptRecord record = response.getBody().get(0);
        assertThat(record.orderId()).isEqualTo("1001");
        assertThat(record.paymentId()).startsWith("pay-");
        assertThat(record.pgTransactionId()).startsWith("pg-");
    }

    @Test
    void getOrder_returnsOrderResponse() {
        paymentController.process(null, new PaymentDto.Request("1001", "cust-001", 15_000L));

        ResponseEntity<OrderDto.Response> response = paymentController.getOrder("1001");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isEqualTo("1001");
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PAID);
        assertThat(response.getBody().lastPaymentId()).startsWith("pay-");
        assertThat(response.getBody().lastPgTransactionId()).startsWith("pg-");
    }

    @Test
    void currentOrder_returnsLatestPendingOrder() {
        ResponseEntity<OrderDto.Response> response = paymentController.getCurrentOrder();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isEqualTo("1001");
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createNextOrder_returnsNewPendingOrder() {
        ResponseEntity<OrderDto.Response> response = paymentController.createNextOrder();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderId()).isEqualTo("1002");
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PENDING);
    }
}
