package com.example.idempotency.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class MockPgService {

    private final long pgDelayMs;

    public MockPgService(@Value("${payment.pg-delay-ms:500}") long pgDelayMs) {
        this.pgDelayMs = pgDelayMs;
    }

    public PgApprovalResult approve(String orderId, long amount) {
        sleep(pgDelayMs);
        return new PgApprovalResult("pg-" + UUID.randomUUID(), Instant.now());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for PG approval", ex);
        }
    }

    public record PgApprovalResult(String pgTransactionId, Instant approvedAt) {
    }
}

