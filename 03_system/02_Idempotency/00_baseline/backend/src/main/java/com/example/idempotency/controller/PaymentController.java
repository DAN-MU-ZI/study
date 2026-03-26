package com.example.idempotency.controller;

import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentDto.Response> process(@RequestBody PaymentDto.Request request) {
        return ResponseEntity.ok(paymentService.process(request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentAttemptRecord>> getPayments(@RequestParam(value = "orderId", required = false) String orderId) {
        return ResponseEntity.ok(paymentService.getPayments(orderId));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDto.Response> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getOrder(orderId));
    }

    @GetMapping("/orders/current")
    public ResponseEntity<OrderDto.Response> getCurrentOrder() {
        return ResponseEntity.ok(paymentService.getCurrentOrder());
    }

    @PostMapping("/orders/next")
    public ResponseEntity<OrderDto.Response> createNextOrder() {
        return ResponseEntity.ok(paymentService.createNextOrder());
    }
}
