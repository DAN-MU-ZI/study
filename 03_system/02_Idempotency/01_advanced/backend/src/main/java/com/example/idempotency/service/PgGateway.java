package com.example.idempotency.service;

import java.time.Instant;

public interface PgGateway {

    PgApprovalResult approve(String cartId, long amount);

    record PgApprovalResult(String pgTransactionId, Instant approvedAt) {
    }
}
