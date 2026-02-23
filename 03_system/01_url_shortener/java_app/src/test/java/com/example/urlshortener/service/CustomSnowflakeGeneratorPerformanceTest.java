package com.example.urlshortener.service;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomSnowflakeGeneratorPerformanceTest {

    @Test
    public void testSnowflakePerformance() throws InterruptedException {
        CustomSnowflakeGenerator generator = new CustomSnowflakeGenerator(1L);

        int threadCount = 100;
        int iterationsPerThread = 100; // 100 * 100 = 10,000 IDs
        int totalIterations = threadCount * iterationsPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicLong generatedCount = new AtomicLong(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        generator.nextId();
                        generatedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        System.out.println("Starting load test (before/after)...");
        long startTime = System.currentTimeMillis();
        
        startLatch.countDown();
        
        boolean finished = endLatch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executorService.shutdown();

        long duration = endTime - startTime;
        System.out.println("Generated " + generatedCount.get() + " IDs in " + duration + " ms.");
        System.out.println("Throughput: " + (generatedCount.get() * 1000L / Math.max(1, duration)) + " IDs/sec");
        
        assertTrue(finished, "Test timed out");
        assertEquals(totalIterations, generatedCount.get());
    }
}
