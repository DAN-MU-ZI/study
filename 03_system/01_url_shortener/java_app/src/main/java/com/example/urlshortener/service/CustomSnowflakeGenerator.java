package com.example.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CustomSnowflakeGenerator {

    // 2024년 1월 1일 00:00:00 UTC (초 단위 Epoch)
    private static final long CUSTOM_EPOCH = 1704067200L; 

    // ✅ 변경점: 워커 2bit, 시퀀스 11bit
    private static final long WORKER_ID_BITS = 2L;
    private static final long SEQUENCE_BITS = 11L;

    // 최대 워커 ID: 3, 최대 시퀀스: 2047
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // 11
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 13

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public CustomSnowflakeGenerator(@Value("${app.snowflake.worker-id}") long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("Worker ID는 0에서 %d 사이여야 합니다.", MAX_WORKER_ID)
            );
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("시스템 시간이 역행했습니다.");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            
            // ✅ 이제 초당 2,048번을 넘겨야만 이 대기 로직을 탑니다.
            if (sequence == 0) {
                currentTimestamp = waitNextSecond(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long getCurrentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    private long waitNextSecond(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }
}
