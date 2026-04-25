package com.example.backend.monitoring.service;

import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import com.example.backend.monitoring.support.ReminderHealthTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderHealthService {

    private final ReminderHealthTracker reminderHealthTracker;

    public ReminderHealthResponse getReminderHealth() {
        LocalDateTime lastRunAt = reminderHealthTracker.getLastRunAt();
        LocalDateTime lastSuccessAt = reminderHealthTracker.getLastSuccessAt();
        LocalDateTime lastFailureAt = reminderHealthTracker.getLastFailureAt();
        String lastFailureMessage = reminderHealthTracker.getLastFailureMessage();
        long recentCreatedCount = reminderHealthTracker.getRecentCreatedCount();

        long minutesSinceLastRun = -1;
        if (lastRunAt != null) {
            minutesSinceLastRun = Duration.between(lastRunAt, LocalDateTime.now()).toMinutes();
        }

        List<String> notes = new ArrayList<>();
        HealthStatus status = HealthStatus.UP;

        if (lastRunAt == null) {
            status = HealthStatus.DEGRADED;
            notes.add("아직 reminder scheduler 실행 이력이 없습니다.");
        } else {
            notes.add("최근 scheduler 실행 시각이 기록되어 있습니다.");
            if (minutesSinceLastRun > 5) {
                status = HealthStatus.DEGRADED;
                notes.add("마지막 scheduler 실행이 5분 이상 지났습니다.");
            }
        }

        if (lastSuccessAt == null) {
            notes.add("아직 scheduler 성공 이력이 없습니다.");
        } else {
            notes.add("최근 scheduler 성공 이력이 있습니다.");
        }

        if (lastFailureAt != null) {
            status = HealthStatus.DOWN;
            notes.add("마지막 scheduler 실행 중 오류가 발생했습니다.");
            if (lastFailureMessage != null && !lastFailureMessage.isBlank()) {
                notes.add("lastFailureMessage=" + lastFailureMessage);
            }
        }

        return new ReminderHealthResponse(
                status,
                true,
                lastRunAt,
                lastSuccessAt,
                lastFailureAt,
                lastFailureMessage,
                recentCreatedCount,
                minutesSinceLastRun,
                notes,
                LocalDateTime.now()
        );
    }
}
