package com.example.backend.monitoring.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReminderHealthTracker {

    private final AtomicReference<LocalDateTime> lastRunAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastSuccessAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastFailureAt = new AtomicReference<>(null);
    private final AtomicReference<String> lastFailureMessage = new AtomicReference<>(null);
    private final AtomicLong recentCreatedCount = new AtomicLong(0);

    public void markRunStarted() {
        lastRunAt.set(LocalDateTime.now());
    }

    public void markRunSuccess(long createdCount) {
        lastSuccessAt.set(LocalDateTime.now());
        lastFailureAt.set(null);
        lastFailureMessage.set(null);
        recentCreatedCount.set(createdCount);
    }

    public void markRunFailure(Exception exception) {
        lastFailureAt.set(LocalDateTime.now());
        lastFailureMessage.set(toSafeMessage(exception));
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt.get();
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt.get();
    }

    public LocalDateTime getLastFailureAt() {
        return lastFailureAt.get();
    }

    public String getLastFailureMessage() {
        return lastFailureMessage.get();
    }

    public long getRecentCreatedCount() {
        return recentCreatedCount.get();
    }

    public void reset() {
        lastRunAt.set(null);
        lastSuccessAt.set(null);
        lastFailureAt.set(null);
        lastFailureMessage.set(null);
        recentCreatedCount.set(0);
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
