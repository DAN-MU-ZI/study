package com.example.urlshortener.lab;

public interface SnowflakeGenerator {

    long CUSTOM_EPOCH = 1704067200L;
    long WORKER_ID_BITS = 2L;
    long SEQUENCE_BITS = 11L;
    long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    long WORKER_ID_SHIFT = SEQUENCE_BITS;
    long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    long nextId();

    default long retryCount() {
        return 0L;
    }
}
