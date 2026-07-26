package com.example.urlshortener.lab;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public final class FirstStateReadBarrier implements StateReadHook {

    private final CyclicBarrier barrier;
    private final ThreadLocal<Boolean> firstRead = ThreadLocal.withInitial(() -> true);

    public FirstStateReadBarrier(int concurrentReaders) {
        this.barrier = new CyclicBarrier(concurrentReaders);
    }

    @Override
    public void afterRead() {
        if (!firstRead.get()) {
            return;
        }

        firstRead.set(false);
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("동시 상태 읽기 지점을 맞추지 못했습니다.", e);
        }
    }
}
