package com.example.idempotency.service;

import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.OrderStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private OrderStore orderStore;
    private PaymentStore paymentStore;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderStore = new OrderStore("1001", "cust-001", 15_000L);
        paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(0L);
        paymentService = new PaymentService(orderStore, paymentStore, mockPgService, 0L);
    }

    @Test
    void singleRequest_marksOrderPaid_andCreatesOneAttempt() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));

        assertThat(orderStore.get("1001").status()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentStore.findAll()).hasSize(1);

        PaymentAttemptRecord record = paymentStore.findAll().get(0);
        assertThat(record.orderId()).isEqualTo("1001");
        assertThat(record.status()).isEqualTo("PAID");
        assertThat(record.pgTransactionId()).startsWith("pg-");
    }

    @Test
    void nonPendingOrder_returnsConflict() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));

        assertThatThrownBy(() -> paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void createNextOrder_returnsNewPendingOrder() {
        OrderStatus nextStatus = OrderStatus.valueOf(paymentService.createNextOrder().status());

        assertThat(paymentService.getCurrentOrder().orderId()).isEqualTo("1002");
        assertThat(nextStatus).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getPayments_filtersByOrderId() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));
        paymentService.createNextOrder();

        assertThat(paymentService.getPayments("1001")).hasSize(1);
        assertThat(paymentService.getPayments("1002")).isEmpty();
    }
}

