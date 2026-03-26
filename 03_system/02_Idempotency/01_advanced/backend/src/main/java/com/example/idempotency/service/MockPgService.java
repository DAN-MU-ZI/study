package com.example.idempotency.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockPgService implements PgGateway {

    private final long pgDelayMs;

    public MockPgService(@Value("${payment.pg-delay-ms:500}") long pgDelayMs) {
        this.pgDelayMs = pgDelayMs;
    }

    @Override
    public PgApprovalResult approve(String orderId, long amount) {
        sleep(pgDelayMs);
        return new PgApprovalResult("pg-" + UUID.randomUUID(), java.time.Instant.now());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for PG approval", ex);
        }
    }
}
