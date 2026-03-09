package com.example.backend.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ReminderHealthResponse {

    private HealthStatus status;

    private boolean schedulerEnabled;
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessAt;
    private long recentCreatedCount;
    private long minutesSinceLastRun;

    private List<String> notes;
    private LocalDateTime serverTime;
}