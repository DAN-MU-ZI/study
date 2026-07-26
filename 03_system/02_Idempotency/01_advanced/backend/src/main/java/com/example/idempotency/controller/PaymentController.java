package com.example.idempotency.controller;

import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<PaymentDto.Response> process(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody PaymentDto.Request request
    ) {
        return ResponseEntity.ok(paymentService.process(idempotencyKey, request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentAttemptRecord>> getPayments(
        @RequestParam(value = "cartId", required = false) String cartId,
        @RequestParam(value = "orderId", required = false) String legacyOrderId
    ) {
        return ResponseEntity.ok(paymentService.getPayments(resolveCartScope(cartId, legacyOrderId)));
    }

    private String resolveCartScope(String cartId, String legacyOrderId) {
        String normalizedCartId = normalize(cartId);
        return normalizedCartId != null ? normalizedCartId : normalize(legacyOrderId);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
