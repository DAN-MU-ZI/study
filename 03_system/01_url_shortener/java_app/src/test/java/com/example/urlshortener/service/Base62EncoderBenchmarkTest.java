package com.example.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

class Base62EncoderBenchmarkTest {

    private static final String BASE62_STR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] BASE62 = BASE62_STR.toCharArray();

    @Test
    void compareBaselineVsPrimitiveEncoder() {
        final int size = 1_000_000;
        final int rounds = 5;
        final long[] ids = new long[size];
        SplittableRandom random = new SplittableRandom(42);
        for (int i = 0; i < size; i++) {
            ids[i] = random.nextLong(1, Long.MAX_VALUE);
        }

        long blackhole = 0;

        for (int i = 0; i < 3; i++) {
            blackhole += runBaseline(ids);
            blackhole += runPrimitive(ids);
        }

        long baselineTotalNs = 0;
        long primitiveTotalNs = 0;
        long baselineMinNs = Long.MAX_VALUE;
        long primitiveMinNs = Long.MAX_VALUE;
        for (int i = 0; i < rounds; i++) {
            long baselineNs = runBaseline(ids);
            long primitiveNs = runPrimitive(ids);
            baselineTotalNs += baselineNs;
            primitiveTotalNs += primitiveNs;
            baselineMinNs = Math.min(baselineMinNs, baselineNs);
            primitiveMinNs = Math.min(primitiveMinNs, primitiveNs);
        }

        double baselineAvgPerOpNs = (double) baselineTotalNs / rounds / size;
        double primitiveAvgPerOpNs = (double) primitiveTotalNs / rounds / size;
        double baselineMinPerOpNs = (double) baselineMinNs / size;
        double primitiveMinPerOpNs = (double) primitiveMinNs / size;
        double speedup = baselineAvgPerOpNs / primitiveAvgPerOpNs;

        System.out.println("=== Base62 Encode Benchmark (1,000,000 ops, rounds=5) ===");
        System.out.printf("baseline avg: %.2f ns/op%n", baselineAvgPerOpNs);
        System.out.printf("baseline min: %.2f ns/op%n", baselineMinPerOpNs);
        System.out.printf("primitive avg: %.2f ns/op%n", primitiveAvgPerOpNs);
        System.out.printf("primitive min: %.2f ns/op%n", primitiveMinPerOpNs);
        System.out.printf("speedup(avg): %.2fx%n", speedup);
        System.out.println("blackhole : " + blackhole);
    }

    private long runBaseline(long[] ids) {
        long checksum = 0;
        long start = System.nanoTime();
        for (long id : ids) {
            checksum += baselineEncode(id).length();
        }
        long end = System.nanoTime();
        if (checksum == Long.MIN_VALUE) {
            throw new IllegalStateException("unreachable");
        }
        return end - start;
    }

    private long runPrimitive(long[] ids) {
        long checksum = 0;
        long start = System.nanoTime();
        for (long id : ids) {
            checksum += primitiveEncode(id).length();
        }
        long end = System.nanoTime();
        if (checksum == Long.MIN_VALUE) {
            throw new IllegalStateException("unreachable");
        }
        return end - start;
    }

    private String baselineEncode(long id) {
        StringBuilder sb = new StringBuilder();
        long num = id;

        if (num == 0) {
            return String.valueOf(BASE62_STR.charAt(0));
        }

        while (num > 0) {
            sb.append(BASE62_STR.charAt((int) (num % 62)));
            num /= 62;
        }

        return sb.reverse().toString();
    }

    private String primitiveEncode(long id) {
        if (id == 0) {
            return "0";
        }

        char[] buffer = new char[11];
        int pos = buffer.length;
        long num = id;
        while (num > 0) {
            long q = num / 62;
            int r = (int) (num - q * 62);
            buffer[--pos] = BASE62[r];
            num = q;
        }

        return new String(buffer, pos, buffer.length - pos);
    }
}
