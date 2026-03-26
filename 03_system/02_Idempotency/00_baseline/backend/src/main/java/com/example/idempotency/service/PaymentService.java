package com.example.idempotency.service;

import com.example.idempotency.domain.OrderRecord;
import com.example.idempotency.domain.OrderStatus;
import com.example.idempotency.domain.PaymentAttemptRecord;
import com.example.idempotency.dto.OrderDto;
import com.example.idempotency.dto.PaymentDto;
import com.example.idempotency.store.OrderStore;
import com.example.idempotency.store.PaymentStore;
import org.springframework.beans.factory.annotation.Value;
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
    private final MockPgService mockPgService;
    private final long processingDelayMs;

    public PaymentService(
        OrderStore orderStore,
        PaymentStore paymentStore,
        MockPgService mockPgService,
        @Value("${payment.processing-delay-ms:700}") long processingDelayMs
    ) {
        this.orderStore = orderStore;
        this.paymentStore = paymentStore;
        this.mockPgService = mockPgService;
        this.processingDelayMs = processingDelayMs;
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
        sleep(processingDelayMs);

        MockPgService.PgApprovalResult approval = mockPgService.approve(request.orderId(), request.amount());
        String paymentId = "pay-" + UUID.randomUUID();
        String threadName = Thread.currentThread().getName();

        paymentStore.add(new PaymentAttemptRecord(
            request.orderId(),
            request.customerId(),
            request.amount(),
            paymentId,
            approval.pgTransactionId(),
            OrderStatus.PAID.name(),
            threadName,
            requestedAt,
            approval.approvedAt()
        ));

        orderStore.markPaid(request.orderId(), paymentId, approval.pgTransactionId());

        return new PaymentDto.Response(
            request.orderId(),
            paymentId,
            approval.pgTransactionId(),
            OrderStatus.PAID.name(),
            approval.approvedAt(),
            threadName
        );
    }

    private OrderDto.Response toOrderResponse(OrderRecord order) {
        return new OrderDto.Response(
            order.orderId(),
            order.status().name(),
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while processing payment", ex);
        }
    }
}
