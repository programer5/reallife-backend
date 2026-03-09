package com.example.backend.monitoring.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReminderHealthTracker {

    private final AtomicReference<LocalDateTime> lastRunAt = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> lastSuccessAt = new AtomicReference<>(null);
    private final AtomicLong recentCreatedCount = new AtomicLong(0);

    public void markRunStarted() {
        lastRunAt.set(LocalDateTime.now());
    }

    public void markRunSuccess(long createdCount) {
        lastSuccessAt.set(LocalDateTime.now());
        recentCreatedCount.set(createdCount);
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt.get();
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt.get();
    }

    public long getRecentCreatedCount() {
        return recentCreatedCount.get();
    }
}