package com.example.idempotency.service;

import com.example.idempotency.domain.CartRecord;
import com.example.idempotency.dto.CartDto;
import com.example.idempotency.store.CartStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {

    private final CartStore cartStore;

    public CartService(CartStore cartStore) {
        this.cartStore = cartStore;
    }

    public CartDto.Response getCart(String cartId) {
        CartRecord cart = cartStore.get(cartId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found: " + cartId);
        }
        return toResponse(cart);
    }

    public CartDto.Response getCurrentCart() {
        return toResponse(cartStore.getCurrentCart());
    }

    public CartDto.Response createNextCart() {
        return toResponse(cartStore.createNextCart());
    }

    private CartDto.Response toResponse(CartRecord cart) {
        return new CartDto.Response(
            cart.cartId(),
            cart.status(),
            cart.lastPaymentId(),
            cart.lastPgTransactionId()
        );
    }
}
