package com.example.backend.monitoring.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SseHealthTracker {

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastRegisteredAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastDisconnectedAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastEventSentAt = new AtomicReference<>(null);

    public void onConnected() {
        activeConnections.incrementAndGet();
        lastRegisteredAt.set(LocalDateTime.now());
    }

    public void onDisconnected() {
        int updated = activeConnections.decrementAndGet();
        if (updated < 0) {
            activeConnections.set(0);
        }
        lastDisconnectedAt.set(LocalDateTime.now());
    }

    public void onEventSent() {
        lastEventSentAt.set(LocalDateTime.now());
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public LocalDateTime getLastRegisteredAt() {
        return lastRegisteredAt.get();
    }

    public LocalDateTime getLastDisconnectedAt() {
        return lastDisconnectedAt.get();
    }

    public LocalDateTime getLastEventSentAt() {
        return lastEventSentAt.get();
    }

    public void reset() {
        activeConnections.set(0);
        lastRegisteredAt.set(null);
        lastDisconnectedAt.set(null);
        lastEventSentAt.set(null);
    }
}