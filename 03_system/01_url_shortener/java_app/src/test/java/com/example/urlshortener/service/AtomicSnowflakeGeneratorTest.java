package com.example.urlshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class AtomicSnowflakeGeneratorTest {

    @Test
    void shouldRejectWorkerIdOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> new AtomicSnowflakeGenerator(-1L));
        assertThrows(IllegalArgumentException.class, () -> new AtomicSnowflakeGenerator(4L));
    }

    @Test
    void shouldGenerateUniqueIdsWhenCalledConcurrently() throws Exception {
        AtomicSnowflakeGenerator generator = new AtomicSnowflakeGenerator(1L);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        List<Callable<Long>> tasks = java.util.stream.IntStream.range(0, 1024)
                .<Callable<Long>>mapToObj(index -> generator::nextId)
                .toList();

        try {
            Set<Long> ids = new HashSet<>();
            executor.invokeAll(tasks).forEach(future -> {
                try {
                    ids.add(future.get());
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });

            assertEquals(1024, ids.size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
