package com.coda.roundrobin.httphandler.circuitbreaker;

public class CircuitBreaker {
    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_TIMEOUT_MS = 10_000; // 10 seconds

    private int failureCount = 0;
    private long lastFailureTime = 0;
    private boolean open = false;

    public synchronized boolean isAvailable() {
        if (!open) return true;
        // Try half-open after timeout
        return (System.currentTimeMillis() - lastFailureTime) > OPEN_TIMEOUT_MS;
    }

    public synchronized void recordSuccess() {
        failureCount = 0;
        open = false;
    }

    public synchronized void recordFailure() {
        failureCount++;
        if (failureCount >= FAILURE_THRESHOLD) {
            open = true;
            lastFailureTime = System.currentTimeMillis();
        }
    }
}

