package com.example.shopify_legacy.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.shopify_legacy.checkout.CheckoutLine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final StringRedisTemplate redisTemplate;
    private final ReservationRepository reservationRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;

    public Reservation reserve(Long checkoutId, List<CheckoutLine> lines) {
        String reservationToken = UUID.randomUUID().toString();
        List<CheckoutLine> reservedLines = new ArrayList<>();

        try {
            for (CheckoutLine line : lines) {
                String stockKey = toStockKey(line.inventoryItemId());

                Long remaining = redisTemplate.opsForValue()
                        .decrement(stockKey, line.quantity());

                if (remaining == null || remaining < 0) {
                    redisTemplate.opsForValue()
                            .increment(stockKey, line.quantity());

                    throw new IllegalStateException("INSUFFICIENT_STOCK");
                }

                reservedLines.add(line);
            }

            redisTemplate.opsForValue().set(
                    toReservationKey(reservationToken),
                    "RESERVED"
            );

            Reservation reservation = Reservation.reserved(
                    reservationToken,
                    checkoutId,
                    lines
            );
            return reservationRepository.save(reservation);
        } catch (RuntimeException e) {
            for (CheckoutLine line : reservedLines) {
                redisTemplate.opsForValue()
                        .increment(
                                toStockKey(line.inventoryItemId()),
                                line.quantity()
                        );
            }

            redisTemplate.delete(toReservationKey(reservationToken));
            throw e;
        }
    }

    public void claim(Long reservationId, String paymentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow();

        if (reservation.isClaimed()) {
            return;
        }

        if (reservation.isReleased()) {
            throw new IllegalStateException("ALREADY_RELEASED");
        }

        for (ReservationLine line : reservation.getLines()) {
            InventoryLedger ledger = InventoryLedger.claim(
                    line.getInventoryItemId(),
                    line.getQuantity(),
                    paymentId
            );

            inventoryLedgerRepository.save(ledger);
        }

        cleanupRedisReservation(reservation);

        reservation.claim();
        reservationRepository.save(reservation);
    }

    public void claimCleanupFirstForFailureDemo(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow();

        cleanupRedisReservation(reservation);

        throw new IllegalStateException("LEDGER_DEDUCTION_FAILED");
    }

    public void release(Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow();

        if (reservation.isClaimed() || reservation.isReleased()) {
            return;
        }

        for (ReservationLine line : reservation.getLines()) {
            redisTemplate.opsForValue()
                    .increment(
                            toStockKey(line.getInventoryItemId()),
                            line.getQuantity()
                    );
        }

        reservation.release(reason);
        reservationRepository.save(reservation);
    }

    private String toStockKey(Long inventoryItemId) {
        return "stock:" + inventoryItemId;
    }

    private String toReservationKey(String reservationToken) {
        return "reservation:" + reservationToken;
    }

    private void cleanupRedisReservation(Reservation reservation) {
        redisTemplate.delete(toReservationKey(reservation.getReservationToken()));
    }
}
