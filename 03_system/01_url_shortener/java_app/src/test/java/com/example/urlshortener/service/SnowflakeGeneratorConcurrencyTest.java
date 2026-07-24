package com.example.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeGeneratorConcurrencyTest {

    @Test
    void nonAtomicGeneratorShouldProduceDuplicateIdsWhenStateReadsOverlap() throws Exception {
        NonAtomicSnowflakeGenerator generator = new NonAtomicSnowflakeGenerator(1L, 2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Long> first = executor.submit(generator::nextId);
            Future<Long> second = executor.submit(generator::nextId);

            long firstId = first.get(5, TimeUnit.SECONDS);
            long secondId = second.get(5, TimeUnit.SECONDS);

            generator.getTraces().stream()
                    .sorted(Comparator.comparing(GenerationTrace::threadName))
                    .forEach(trace -> System.out.printf(
                            "[non-atomic] thread=%s, readState=%d, sequence=%d, nextState=%d, generatedId=%d%n",
                            trace.threadName(),
                            trace.readState(),
                            trace.sequence(),
                            trace.nextState(),
                            trace.generatedId()
                    ));
            System.out.printf(
                    "[non-atomic] firstId=%d, secondId=%d, duplicate=%s%n",
                    firstId,
                    secondId,
                    firstId == secondId
            );

            assertEquals(firstId, secondId, "비원자 상태 갱신에서는 중복 ID가 발생해야 합니다.");
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "작업 스레드가 종료되지 않았습니다.");
        }
    }

    @Test
    void shouldGenerateUniqueIdsWhenCalledConcurrently() throws Exception {
        SnowflakeGenerator generator = new SnowflakeGenerator(1L);
        int threadCount = 64;
        int idsPerThread = 16;
        int expectedIdCount = threadCount * idsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();

                    List<Long> generatedIds = new ArrayList<>(idsPerThread);
                    for (int j = 0; j < idsPerThread; j++) {
                        generatedIds.add(generator.nextId());
                    }
                    return generatedIds;
                }));
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "모든 작업 스레드가 준비되지 않았습니다.");
            startLatch.countDown();

            Set<Long> uniqueIds = new HashSet<>();
            int generatedIdCount = 0;

            for (Future<List<Long>> future : futures) {
                List<Long> threadIds = future.get(10, TimeUnit.SECONDS);
                generatedIdCount += threadIds.size();
                uniqueIds.addAll(threadIds);

                for (int i = 1; i < threadIds.size(); i++) {
                    assertTrue(
                            threadIds.get(i - 1) < threadIds.get(i),
                            "같은 스레드에서 나중에 생성한 ID가 더 커야 합니다."
                    );
                }
            }

            assertEquals(expectedIdCount, generatedIdCount, "요청한 수만큼 ID가 생성되어야 합니다.");
            assertEquals(expectedIdCount, uniqueIds.size(), "동시에 생성한 ID에 중복이 없어야 합니다.");
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "작업 스레드가 종료되지 않았습니다.");
        }
    }

    private static class NonAtomicSnowflakeGenerator {

        private static final long CUSTOM_EPOCH = 1704067200L;
        private static final long SEQUENCE_BITS = 11L;
        private static final long WORKER_ID_BITS = 2L;
        private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
        private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        private static final long FIXED_TIMESTAMP = CUSTOM_EPOCH + 1;

        private final long workerId;
        private final CyclicBarrier stateReadBarrier;
        private final Queue<GenerationTrace> traces = new ConcurrentLinkedQueue<>();
        private long state;

        private NonAtomicSnowflakeGenerator(long workerId, int concurrentReaders) {
            this.workerId = workerId;
            this.stateReadBarrier = new CyclicBarrier(concurrentReaders);
        }

        private long nextId() throws Exception {
            long currentState = state;

            // 두 스레드가 같은 state를 읽은 뒤 상태 계산을 진행하도록 경합 시점을 고정한다.
            stateReadBarrier.await(5, TimeUnit.SECONDS);

            long lastTimestamp = currentState >>> SEQUENCE_BITS;
            long sequence = currentState & MAX_SEQUENCE;
            if (FIXED_TIMESTAMP == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
            } else {
                sequence = 0L;
            }

            long nextState = (FIXED_TIMESTAMP << SEQUENCE_BITS) | sequence;
            state = nextState;

            long generatedId = ((FIXED_TIMESTAMP - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                    | (workerId << SEQUENCE_BITS)
                    | sequence;
            traces.add(new GenerationTrace(
                    Thread.currentThread().getName(),
                    currentState,
                    sequence,
                    nextState,
                    generatedId
            ));
            return generatedId;
        }

        private List<GenerationTrace> getTraces() {
            return List.copyOf(traces);
        }
    }

    private record GenerationTrace(
            String threadName,
            long readState,
            long sequence,
            long nextState,
            long generatedId
    ) {
    }
}
