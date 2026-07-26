package com.example.idempotency.controller;

import com.example.idempotency.dto.CartDto;
import com.example.idempotency.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping({"/carts/{cartId}", "/orders/{cartId}"})
    public ResponseEntity<CartDto.Response> getCart(@PathVariable String cartId) {
        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @GetMapping({"/carts/current", "/orders/current"})
    public ResponseEntity<CartDto.Response> getCurrentCart() {
        return ResponseEntity.ok(cartService.getCurrentCart());
    }

    @PostMapping({"/carts/next", "/orders/next"})
    public ResponseEntity<CartDto.Response> createNextCart() {
        return ResponseEntity.ok(cartService.createNextCart());
    }
}
