package com.example.shopify_legacy.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.shopify_legacy.inventory.InventoryReservationService;
import com.example.shopify_legacy.inventory.Reservation;
import com.example.shopify_legacy.payment.PaymentResult;
import com.example.shopify_legacy.payment.PaymentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("체크아웃 완료 서비스")
class CheckoutServiceTest {

    @Mock
    private InventoryReservationService inventoryReservationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderService orderService;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(
                inventoryReservationService,
                paymentService,
                orderService
        );
    }

    @Test
    @DisplayName("결제가 성공하면 예약을 확정하고 주문을 생성한다")
    void completeClaimsReservationAndCreatesOrderWhenPaymentSucceeds() {
        CheckoutCompleteRequest request = new CheckoutCompleteRequest(
                List.of(new CheckoutLine(100L, 2)),
                "token"
        );
        Reservation reservation = mock(Reservation.class);
        Order order = mock(Order.class);

        when(reservation.getId()).thenReturn(10L);
        when(order.getId()).thenReturn(20L);
        when(inventoryReservationService.reserve(1L, request.lines())).thenReturn(reservation);
        when(paymentService.pay(1L, "token")).thenReturn(new PaymentResult(true, "pay_1"));
        when(orderService.createOrder(1L, "pay_1")).thenReturn(order);

        CheckoutCompleteResponse response = checkoutService.complete(1L, request);

        verify(inventoryReservationService).claim(10L, "pay_1");
        verify(orderService).createOrder(1L, "pay_1");
        assertThat(response.orderId()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo("completed");
    }

    @Test
    @DisplayName("결제가 실패하면 예약을 해제하고 주문을 생성하지 않는다")
    void completeReleasesReservationWhenPaymentFails() {
        CheckoutCompleteRequest request = new CheckoutCompleteRequest(
                List.of(new CheckoutLine(100L, 2)),
                "fail"
        );
        Reservation reservation = mock(Reservation.class);

        when(reservation.getId()).thenReturn(10L);
        when(inventoryReservationService.reserve(1L, request.lines())).thenReturn(reservation);
        when(paymentService.pay(1L, "fail")).thenReturn(new PaymentResult(false, null));

        assertThatThrownBy(() -> checkoutService.complete(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PAYMENT_FAILED");

        verify(inventoryReservationService).release(10L, "payment_failed");
        verify(inventoryReservationService, never()).claim(eq(10L), anyString());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("결제 처리 중 예외가 발생하면 예약을 해제한다")
    void completeReleasesReservationWhenPaymentThrows() {
        CheckoutCompleteRequest request = new CheckoutCompleteRequest(
                List.of(new CheckoutLine(100L, 2)),
                "token"
        );
        Reservation reservation = mock(Reservation.class);
        RuntimeException paymentError = new RuntimeException("payment error");

        when(reservation.getId()).thenReturn(10L);
        when(inventoryReservationService.reserve(1L, request.lines())).thenReturn(reservation);
        when(paymentService.pay(1L, "token")).thenThrow(paymentError);

        assertThatThrownBy(() -> checkoutService.complete(1L, request))
                .isSameAs(paymentError);

        verify(inventoryReservationService).release(10L, "payment_error");
        verify(inventoryReservationService, never()).claim(eq(10L), anyString());
        verifyNoInteractions(orderService);
    }
}
