package com.example.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AtomicSnowflakeGenerator implements SnowflakeGenerator {

    // 2024년 1월 1일 00:00:00 UTC (초 단위 Epoch)
    private static final long CUSTOM_EPOCH = 1704067200L;

    // Snowflake: 32bit(29bit timestamp + 2bit worker + 11bit sequence)
    private static final long WORKER_ID_BITS = 2L;
    private static final long SEQUENCE_BITS = 11L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // 11
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 13

    private final long workerId;
    private final AtomicLong state = new AtomicLong(0L);

    public AtomicSnowflakeGenerator(@Value("${app.snowflake.worker-id}") long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID는 0에서 %d 사이여야 합니다.", MAX_WORKER_ID)
            );
        }
        this.workerId = workerId;
    }

    @Override
    public long nextId() {
        while (true) {
            long currentTimestamp = getCurrentTimestamp();
            long currentState = state.get();

            long lastTimestamp = currentState >>> SEQUENCE_BITS;
            long sequence = currentState & MAX_SEQUENCE;

            if (currentTimestamp < lastTimestamp) {
                throw new IllegalStateException("시스템 시간이 역행했습니다.");
            }

            if (currentTimestamp == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;

                if (sequence == 0) {
                    currentTimestamp = waitNextSecond(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            long nextState = (currentTimestamp << SEQUENCE_BITS) | sequence;

            if (state.compareAndSet(currentState, nextState)) {
                return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                        | (workerId << WORKER_ID_SHIFT)
                        | sequence;
            }
        }
    }

    private long getCurrentTimestamp() {
        return Instant.now().getEpochSecond();
    }

    private long waitNextSecond(long currentTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= currentTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}
