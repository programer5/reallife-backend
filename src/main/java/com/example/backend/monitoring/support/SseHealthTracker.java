package com.example.backend.monitoring.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SseHealthTracker {

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastRegisteredAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastDisconnectedAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastEventSentAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastFailureAt = new AtomicReference<>(null);
    private final AtomicReference<String> lastFailureMessage = new AtomicReference<>(null);
    private final AtomicLong failureCount = new AtomicLong(0);

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
        lastFailureAt.set(null);
        lastFailureMessage.set(null);
    }

    public void onFailure(Exception exception) {
        lastFailureAt.set(LocalDateTime.now());
        lastFailureMessage.set(toSafeMessage(exception));
        failureCount.incrementAndGet();
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

    public LocalDateTime getLastFailureAt() {
        return lastFailureAt.get();
    }

    public String getLastFailureMessage() {
        return lastFailureMessage.get();
    }

    public long getFailureCount() {
        return failureCount.get();
    }

    public void reset() {
        activeConnections.set(0);
        lastRegisteredAt.set(null);
        lastDisconnectedAt.set(null);
        lastEventSentAt.set(null);
        lastFailureAt.set(null);
        lastFailureMessage.set(null);
        failureCount.set(0);
    }

    private static String toSafeMessage(Exception exception) {
        if (exception == null) {
            return null;
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
