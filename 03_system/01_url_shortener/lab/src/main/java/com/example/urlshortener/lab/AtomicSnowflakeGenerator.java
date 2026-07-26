package com.example.urlshortener.lab;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

/**
 * 운영 SnowflakeGenerator의 ID 생성 로직을 Spring 의존성 없이 옮긴 CAS 구현입니다.
 * 비교 실험에서 동일한 시각과 경합 지점을 주입할 수 있도록 생성자만 확장했습니다.
 */
public final class AtomicSnowflakeGenerator implements SnowflakeGenerator {

    private final long workerId;
    private final LongSupplier clock;
    private final StateReadHook stateReadHook;
    private final AtomicLong state = new AtomicLong(0L);
    private final LongAdder retries = new LongAdder();

    public AtomicSnowflakeGenerator(long workerId, LongSupplier clock) {
        this(workerId, clock, StateReadHook.NONE);
    }

    public AtomicSnowflakeGenerator(
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
        while (true) {
            long currentTimestamp = clock.getAsLong();
            long currentState = state.get();
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

            long nextState = (currentTimestamp << SEQUENCE_BITS) | sequence;
            if (state.compareAndSet(currentState, nextState)) {
                return composeId(currentTimestamp, sequence);
            }
            retries.increment();
        }
    }

    @Override
    public long retryCount() {
        return retries.sum();
    }

    private long composeId(long timestamp, long sequence) {
        return ((timestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
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
