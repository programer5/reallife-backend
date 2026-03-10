package com.example.backend.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReminderHealthResponse(
        HealthStatus status,
        boolean schedulerEnabled,
        LocalDateTime lastRunAt,
        LocalDateTime lastSuccessAt,
        long recentCreatedCount,
        long minutesSinceLastRun,
        List<String> notes,
        LocalDateTime serverTime
) {
}