package com.example.idempotency.service;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.CartStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private CartStore cartStore;
    private PaymentStore paymentStore;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        cartStore = new CartStore("1001", "cust-001", 15_000L);
        paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(0L);
        paymentService = new PaymentService(cartStore, paymentStore, mockPgService);
    }

    @Test
    void singleRequest_marksCartPaid_andCreatesOneAttempt() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));

        assertThat(cartStore.get("1001").status()).isEqualTo(CartStatus.PAID);
        assertThat(paymentStore.findAll()).hasSize(1);

        PaymentAttemptRecord record = paymentStore.findAll().get(0);
        assertThat(record.cartId()).isEqualTo("1001");
        assertThat(record.status()).isEqualTo(CartStatus.PAID);
        assertThat(record.pgTransactionId()).startsWith("pg-");
    }

    @Test
    void nonPendingCart_returnsConflict() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));

        assertThatThrownBy(() -> paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void missingCartId_returnsBadRequest() {
        assertThatThrownBy(() -> paymentService.process(new PaymentDto.Request("", "cust-001", 15_000L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("cartId is required");
    }

    @Test
    void unknownCart_returnsNotFound() {
        assertThatThrownBy(() -> paymentService.process(new PaymentDto.Request("9999", "cust-001", 15_000L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cart not found: 9999");
    }

    @Test
    void getPayments_filtersByCartId() {
        paymentService.process(new PaymentDto.Request("1001", "cust-001", 15_000L));
        cartStore.createNextCart();

        assertThat(paymentService.getPayments("1001")).hasSize(1);
        assertThat(paymentService.getPayments("1002")).isEmpty();
    }
}
