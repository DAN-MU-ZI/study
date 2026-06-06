package com.example.shopify_legacy.checkout;

import org.springframework.stereotype.Service;

import com.example.shopify_legacy.inventory.InventoryReservationService;
import com.example.shopify_legacy.inventory.Reservation;
import com.example.shopify_legacy.payment.PaymentResult;
import com.example.shopify_legacy.payment.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final InventoryReservationService inventoryReservationService;
    private final PaymentService paymentService;
    private final OrderService orderService;

    public CheckoutCompleteResponse complete(
        Long checkoutId,
        CheckoutCompleteRequest request
    ) {
        Reservation reservation = inventoryReservationService.reserve(
            checkoutId,
            request.lines()
        );

        PaymentResult paymentResult;

        try {
            paymentResult = paymentService.pay(
                checkoutId,
                request.paymentToken()
            );
        } catch (RuntimeException e) {
            inventoryReservationService.release(
                reservation.getId(),
                "payment_error"
            );

            throw e;
        }

        if (!paymentResult.succeeded()) {
            inventoryReservationService.release(
                reservation.getId(),
                "payment_failed"
            );

            throw new IllegalStateException("PAYMENT_FAILED");
        }

        inventoryReservationService.claim(
            reservation.getId(),
            paymentResult.paymentId()
        );

        Order order = orderService.createOrder(
            checkoutId,
            paymentResult.paymentId()
        );

        return new CheckoutCompleteResponse(order.getId(), "completed");
    }
}
