package com.example.shopify_legacy.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.shopify_legacy.checkout.CheckoutLine;

@ExtendWith(MockitoExtension.class)
@DisplayName("재고 예약 서비스")
class InventoryReservationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;

    @Mock
    private InventoryFailureInjector failureInjector;

    private InventoryReservationService inventoryReservationService;

    @BeforeEach
    void setUp() {
        inventoryReservationService = new InventoryReservationService(
                redisTemplate,
                reservationRepository,
                inventoryLedgerRepository,
                failureInjector
        );
    }

    @Test
    @DisplayName("Redis 재고 차감 후 DB 저장이 실패하면 차감한 재고를 복구한다")
    void reserveRestoresRedisStockWhenDatabaseSaveFails() {
        RuntimeException dbFailure = new RuntimeException("db down");
        List<CheckoutLine> lines = List.of(new CheckoutLine(100L, 2));

        stubValueOperations();
        when(valueOperations.decrement("stock:100", 2)).thenReturn(3L);
        when(reservationRepository.save(any(Reservation.class))).thenThrow(dbFailure);

        assertThatThrownBy(() -> inventoryReservationService.reserve(1L, lines))
                .isSameAs(dbFailure);

        verify(valueOperations).decrement("stock:100", 2);
        verify(valueOperations).increment("stock:100", 2);
    }

    @Test
    @DisplayName("재고가 부족하면 실패한 차감을 복구하고 예약을 저장하지 않는다")
    void reserveRestoresFailedLineAndDoesNotSaveWhenStockRunsNegative() {
        List<CheckoutLine> lines = List.of(new CheckoutLine(100L, 2));

        stubValueOperations();
        when(valueOperations.decrement("stock:100", 2)).thenReturn(-1L);

        assertThatThrownBy(() -> inventoryReservationService.reserve(1L, lines))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INSUFFICIENT_STOCK");

        verify(valueOperations).increment("stock:100", 2);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 해제를 재시도해도 재고는 한 번만 복구한다")
    void releaseOnlyRestoresStockOnceWhenRetried() {
        Reservation reservation = Reservation.reserved(
                1L,
                List.of(new CheckoutLine(100L, 2))
        );

        stubValueOperations();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        inventoryReservationService.release(10L, "payment_failed");
        inventoryReservationService.release(10L, "duplicate_release");

        verify(valueOperations, times(1)).increment("stock:100", 2);
        verify(reservationRepository, times(1)).save(reservation);
        assertThat(reservation.isReleased()).isTrue();
        assertThat(reservation.getReleaseReason()).isEqualTo("payment_failed");
    }

    @Test
    @DisplayName("예약 확정을 재시도해도 재고 ledger는 한 번만 저장한다")
    void claimOnlyWritesLedgerOnceWhenRetried() {
        Reservation reservation = Reservation.reserved(
                1L,
                List.of(new CheckoutLine(100L, 2))
        );

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        inventoryReservationService.claim(10L, "pay_1");
        inventoryReservationService.claim(10L, "pay_1");

        ArgumentCaptor<InventoryLedger> ledgerCaptor =
                ArgumentCaptor.forClass(InventoryLedger.class);

        verify(inventoryLedgerRepository, times(1)).save(ledgerCaptor.capture());
        verify(reservationRepository, times(1)).save(reservation);

        InventoryLedger ledger = ledgerCaptor.getValue();
        assertThat(ledger.getInventoryItemId()).isEqualTo(100L);
        assertThat(ledger.getQuantity()).isEqualTo(2);
        assertThat(ledger.getPaymentId()).isEqualTo("pay_1");
        assertThat(reservation.isClaimed()).isTrue();
    }

    @Test
    @DisplayName("언더셀 장애 재현: ledger 저장 후 장애가 나면 Redis 예약이 남는다")
    void undersellWhenFailureHappensAfterLedgerSave() {
        Reservation reservation = Reservation.reserved(
                "reservation-token",
                1L,
                List.of(new CheckoutLine(100L, 1))
        );
        RuntimeException failure = new RuntimeException("FAILED_AFTER_LEDGER_SAVE");

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        doThrow(failure).when(failureInjector).afterLedgerSave();

        assertThatThrownBy(() -> inventoryReservationService.claim(10L, "pay_1"))
                .isSameAs(failure);

        verify(inventoryLedgerRepository).save(any(InventoryLedger.class));
        verify(redisTemplate, never()).delete("reservation:reservation-token");
        verify(reservationRepository, never()).save(reservation);
        assertThat(reservation.isClaimed()).isFalse();
    }

    @Test
    @DisplayName("언더셀 장애 재현: ledger 차감 후 Redis 예약 cleanup이 실패하면 재고가 예약에 묶여 남는다")
    void undersellWhenLedgerDeductionSucceedsBeforeRedisReservationCleanupFails() {
        Reservation reservation = Reservation.reserved(
                "reservation-token",
                1L,
                List.of(new CheckoutLine(100L, 1))
        );
        RuntimeException redisFailure = new RuntimeException("redis down");

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(redisTemplate.delete("reservation:reservation-token")).thenThrow(redisFailure);

        assertThatThrownBy(() -> inventoryReservationService.claim(10L, "pay_1"))
                .isSameAs(redisFailure);

        verify(inventoryLedgerRepository).save(any(InventoryLedger.class));
        verify(reservationRepository, never()).save(reservation);
        assertThat(reservation.isClaimed()).isFalse();
    }

    @Test
    @DisplayName("Redis cleanup 후 장애가 나면 예약 상태가 CLAIMED로 저장되지 않는다")
    void reservationStaysReservedWhenFailureHappensAfterRedisCleanup() {
        Reservation reservation = Reservation.reserved(
                "reservation-token",
                1L,
                List.of(new CheckoutLine(100L, 1))
        );
        RuntimeException failure = new RuntimeException("FAILED_AFTER_REDIS_CLEANUP");

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        doThrow(failure).when(failureInjector).afterRedisCleanup();

        assertThatThrownBy(() -> inventoryReservationService.claim(10L, "pay_1"))
                .isSameAs(failure);

        verify(inventoryLedgerRepository).save(any(InventoryLedger.class));
        verify(redisTemplate).delete("reservation:reservation-token");
        verify(reservationRepository, never()).save(reservation);
        assertThat(reservation.isClaimed()).isFalse();
    }

    private void stubValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
