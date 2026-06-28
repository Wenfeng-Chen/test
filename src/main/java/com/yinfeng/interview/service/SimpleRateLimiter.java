package com.yinfeng.interview.service;

/**
 * 简单令牌桶，用于 RPS 模式限速（无第三方依赖）。
 */
final class SimpleRateLimiter {

    private final double permitsPerSecond;
    private double storedPermits;
    private long lastNanos;

    SimpleRateLimiter(double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond must be positive");
        }
        this.permitsPerSecond = permitsPerSecond;
        this.storedPermits = permitsPerSecond;
        this.lastNanos = System.nanoTime();
    }

    synchronized void acquire() {
        long now = System.nanoTime();
        double elapsed = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        storedPermits = Math.min(permitsPerSecond, storedPermits + elapsed * permitsPerSecond);
        if (storedPermits < 1.0) {
            long waitNanos = (long) ((1.0 - storedPermits) / permitsPerSecond * 1_000_000_000L);
            sleepNanos(waitNanos);
            lastNanos = System.nanoTime();
            storedPermits = 0;
        } else {
            storedPermits -= 1.0;
        }
    }

    private static void sleepNanos(long nanos) {
        if (nanos <= 0) {
            return;
        }
        long millis = nanos / 1_000_000L;
        int extraNanos = (int) (nanos % 1_000_000L);
        try {
            Thread.sleep(millis, extraNanos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
