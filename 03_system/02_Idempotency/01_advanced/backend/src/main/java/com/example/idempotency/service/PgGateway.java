package com.example.idempotency.service;

import java.time.Instant;

public interface PgGateway {

    PgApprovalResult approve(String orderId, long amount);

    record PgApprovalResult(String pgTransactionId, Instant approvedAt) {
    }
}
