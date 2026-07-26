package com.example.idempotency.controller;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.dto.CartDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.CartService;
import com.example.idempotency.service.MockPgService;
import com.example.idempotency.service.PaymentService;
import com.example.idempotency.store.CartStore;
import com.example.idempotency.store.PaymentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CartControllerTest {

    private CartController cartController;
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        CartStore cartStore = new CartStore("1001", "cust-001", 15_000L);
        PaymentStore paymentStore = new PaymentStore();
        MockPgService mockPgService = new MockPgService(0L);

        cartController = new CartController(new CartService(cartStore));
        paymentController = new PaymentController(new PaymentService(cartStore, paymentStore, mockPgService));
    }

    @Test
    void getCart_returnsCartResponse() {
        paymentController.process(null, new PaymentDto.Request("1001", "cust-001", 15_000L));

        ResponseEntity<CartDto.Response> response = cartController.getCart("1001");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cartId()).isEqualTo("1001");
        assertThat(response.getBody().status()).isEqualTo(CartStatus.PAID);
        assertThat(response.getBody().lastPaymentId()).startsWith("pay-");
        assertThat(response.getBody().lastPgTransactionId()).startsWith("pg-");
    }

    @Test
    void currentCart_returnsLatestPendingCart() {
        ResponseEntity<CartDto.Response> response = cartController.getCurrentCart();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cartId()).isEqualTo("1001");
        assertThat(response.getBody().status()).isEqualTo(CartStatus.PENDING);
    }

    @Test
    void createNextCart_returnsNewPendingCart() {
        ResponseEntity<CartDto.Response> response = cartController.createNextCart();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cartId()).isEqualTo("1002");
        assertThat(response.getBody().status()).isEqualTo(CartStatus.PENDING);
    }
}
