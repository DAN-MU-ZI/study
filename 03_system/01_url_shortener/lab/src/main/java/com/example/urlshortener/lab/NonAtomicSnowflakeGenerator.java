package com.example.urlshortener.lab;

import java.util.function.LongSupplier;

/**
 * CAS 없이 일반 long 상태를 읽고 쓰는 비교용 구현입니다.
 */
public final class NonAtomicSnowflakeGenerator implements SnowflakeGenerator {

    private final long workerId;
    private final LongSupplier clock;
    private final StateReadHook stateReadHook;
    private long state;

    public NonAtomicSnowflakeGenerator(long workerId, LongSupplier clock) {
        this(workerId, clock, StateReadHook.NONE);
    }

    public NonAtomicSnowflakeGenerator(
            long workerId,
            LongSupplier clock,
            StateReadHook stateReadHook
    ) {
        validateWorkerId(workerId);
        this.workerId = workerId;
        this.clock = clock;
        this.stateReadHook = stateReadHook;
    }

    @Override
    public long nextId() {
        long currentTimestamp = clock.getAsLong();
        long currentState = state;
        stateReadHook.afterRead();

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

        state = (currentTimestamp << SEQUENCE_BITS) | sequence;
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextSecond(long currentTimestamp) {
        long timestamp = clock.getAsLong();
        while (timestamp <= currentTimestamp) {
            timestamp = clock.getAsLong();
        }
        return timestamp;
    }

    private static void validateWorkerId(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID는 0에서 %d 사이여야 합니다.", MAX_WORKER_ID)
            );
        }
    }
}
