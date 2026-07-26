package com.example.idempotency.service;

import com.example.idempotency.config.Idempotent;
import com.example.idempotency.domain.OrderRecord;
import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.OrderStore;
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

    private final OrderStore orderStore;
    private final PaymentStore paymentStore;
    private final PgGateway pgGateway;

    public PaymentService(
        OrderStore orderStore,
        PaymentStore paymentStore,
        PgGateway pgGateway
    ) {
        this.orderStore = orderStore;
        this.paymentStore = paymentStore;
        this.pgGateway = pgGateway;
    }

    @Idempotent
    public PaymentDto.Response process(String idempotencyKey, PaymentDto.Request request) {
        return executePayment(request);
    }

    public PaymentDto.Response process(PaymentDto.Request request) {
        return executePayment(request);
    }

    public List<PaymentAttemptRecord> getPayments(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return paymentStore.findAll();
        }
        return paymentStore.findByOrderId(orderId);
    }

    public OrderDto.Response getOrder(String orderId) {
        OrderRecord order = orderStore.get(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId);
        }
        return toOrderResponse(order);
    }

    public OrderDto.Response getCurrentOrder() {
        return toOrderResponse(orderStore.getCurrentOrder());
    }

    public OrderDto.Response createNextOrder() {
        return toOrderResponse(orderStore.createNextOrder());
    }

    private PaymentDto.Response executePayment(PaymentDto.Request request) {
        validate(request);

        OrderRecord order = orderStore.get(request.orderId());
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + request.orderId());
        }
        if (order.status() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already processed: " + request.orderId());
        }

        Instant requestedAt = Instant.now();

        PgGateway.PgApprovalResult approval = pgGateway.approve(request.orderId(), request.amount());
        String paymentId = "pay-" + UUID.randomUUID();

        try {
            paymentStore.add(new PaymentAttemptRecord(
                request.orderId(),
                request.customerId(),
                request.amount(),
                paymentId,
                approval.pgTransactionId(),
                OrderStatus.PAID,
                requestedAt,
                approval.approvedAt()
            ));
        } catch (DuplicatePaymentAttemptException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }

        orderStore.markPaid(request.orderId(), paymentId, approval.pgTransactionId());

        return new PaymentDto.Response(
            request.orderId(),
            paymentId,
            approval.pgTransactionId(),
            OrderStatus.PAID,
            approval.approvedAt()
        );
    }

    private OrderDto.Response toOrderResponse(OrderRecord order) {
        return new OrderDto.Response(
            order.orderId(),
            order.status(),
            order.lastPaymentId(),
            order.lastPgTransactionId()
        );
    }

    private void validate(PaymentDto.Request request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment request is required");
        }
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerId is required");
        }
        if (request.amount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
    }
}
