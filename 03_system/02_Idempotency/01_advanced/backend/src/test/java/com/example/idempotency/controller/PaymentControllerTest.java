package com.example.idempotency.controller;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.MockPgService;
import com.example.idempotency.service.PaymentService;
import com.example.idempotency.store.CartStore;
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
        CartStore cartStore = new CartStore("1001", "cust-001", 15_000L);
        PaymentStore paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(0L);
        PaymentService paymentService = new PaymentService(cartStore, paymentStore, mockPgService);

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
        assertThat(response.getBody().cartId()).isEqualTo("1001");
        assertThat(response.getBody().paymentId()).startsWith("pay-");
        assertThat(response.getBody().pgTransactionId()).startsWith("pg-");
        assertThat(response.getBody().status()).isEqualTo(CartStatus.PAID);
    }

    @Test
    void getPayments_returnsEvidenceList() {
        paymentController.process(null, new PaymentDto.Request("1001", "cust-001", 15_000L));

        ResponseEntity<List<PaymentAttemptRecord>> response = paymentController.getPayments("1001", null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        PaymentAttemptRecord record = response.getBody().get(0);
        assertThat(record.cartId()).isEqualTo("1001");
        assertThat(record.paymentId()).startsWith("pay-");
        assertThat(record.pgTransactionId()).startsWith("pg-");
    }
}
