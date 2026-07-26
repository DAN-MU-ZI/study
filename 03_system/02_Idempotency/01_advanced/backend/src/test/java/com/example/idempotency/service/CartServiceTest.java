package com.example.idempotency.service;

import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.dto.CartDto;
import com.example.idempotency.store.CartStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartServiceTest {

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(new CartStore("1001", "cust-001", 15_000L));
    }

    @Test
    void currentCart_returnsSeedCart() {
        CartDto.Response current = cartService.getCurrentCart();

        assertThat(current.cartId()).isEqualTo("1001");
        assertThat(current.status()).isEqualTo(CartStatus.PENDING);
    }

    @Test
    void createNextCart_returnsNewPendingCart() {
        CartDto.Response next = cartService.createNextCart();

        assertThat(next.cartId()).isEqualTo("1002");
        assertThat(next.status()).isEqualTo(CartStatus.PENDING);
        assertThat(cartService.getCurrentCart().cartId()).isEqualTo("1002");
    }

    @Test
    void getCart_whenCartIsMissing_throwsNotFound() {
        assertThatThrownBy(() -> cartService.getCart("9999"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Cart not found: 9999");
    }
}
