package com.example.idempotency.service;

import com.example.idempotency.config.Idempotent;
import com.example.idempotency.domain.CartRecord;
import com.example.idempotency.domain.CartStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.CartStore;
import com.example.idempotency.store.PaymentStore;
import com.example.idempotency.store.PaymentStore.DuplicatePaymentAttemptException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final CartStore cartStore;
    private final PaymentStore paymentStore;
    private final PgGateway pgGateway;

    public PaymentService(
        CartStore cartStore,
        PaymentStore paymentStore,
        PgGateway pgGateway
    ) {
        this.cartStore = cartStore;
        this.paymentStore = paymentStore;
        this.pgGateway = pgGateway;
    }

    @Idempotent
    public PaymentDto.Response process(String idempotencyKey, PaymentDto.Request request) {
        return processPayment(request);
    }

    public PaymentDto.Response process(PaymentDto.Request request) {
        return processPayment(request);
    }

    public List<PaymentAttemptRecord> getPayments(String cartId) {
        if (cartId == null || cartId.isBlank()) {
            return paymentStore.findAll();
        }
        return paymentStore.findByCartId(cartId);
    }

    private PaymentDto.Response processPayment(PaymentDto.Request request) {
        validate(request);

        requirePendingCart(request.cartId());
        Instant requestedAt = Instant.now();
        PgGateway.PgApprovalResult approval = pgGateway.approve(request.cartId(), request.amount());
        PaymentAttemptRecord paymentAttempt = createPaymentAttempt(request, approval, requestedAt);
        cartStore.markPaid(request.cartId(), paymentAttempt.paymentId(), paymentAttempt.pgTransactionId());
        return toResponse(paymentAttempt);
    }

    private CartRecord requirePendingCart(String cartId) {
        CartRecord cart = cartStore.get(cartId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found: " + cartId);
        }
        if (cart.status() != CartStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart already checked out: " + cartId);
        }
        return cart;
    }

    private PaymentAttemptRecord createPaymentAttempt(
        PaymentDto.Request request,
        PgGateway.PgApprovalResult approval,
        Instant requestedAt
    ) {
        String paymentId = "pay-" + UUID.randomUUID();

        try {
            PaymentAttemptRecord paymentAttempt = new PaymentAttemptRecord(
                request.cartId(),
                request.customerId(),
                request.amount(),
                paymentId,
                approval.pgTransactionId(),
                CartStatus.PAID,
                requestedAt,
                approval.approvedAt()
            );
            paymentStore.add(paymentAttempt);
            return paymentAttempt;
        } catch (DuplicatePaymentAttemptException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    private PaymentDto.Response toResponse(PaymentAttemptRecord paymentAttempt) {
        return new PaymentDto.Response(
            paymentAttempt.cartId(),
            paymentAttempt.paymentId(),
            paymentAttempt.pgTransactionId(),
            paymentAttempt.status(),
            paymentAttempt.approvedAt()
        );
    }

    private void validate(PaymentDto.Request request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment request is required");
        }
        if (request.cartId() == null || request.cartId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cartId is required");
        }
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required");
        }
        if (request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
    }
}
