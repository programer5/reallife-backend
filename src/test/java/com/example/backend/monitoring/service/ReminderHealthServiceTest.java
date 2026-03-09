package com.example.backend.monitoring.service;

import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.support.ReminderHealthTracker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderHealthServiceTest {

    @Test
    void scheduler_실행과_성공이_기록되면_UP() {
        ReminderHealthTracker tracker = new ReminderHealthTracker();
        tracker.markRunStarted();
        tracker.markRunSuccess(3);

        ReminderHealthService service = new ReminderHealthService(tracker);

        var result = service.getReminderHealth();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.isSchedulerEnabled()).isTrue();
        assertThat(result.getLastRunAt()).isNotNull();
        assertThat(result.getLastSuccessAt()).isNotNull();
        assertThat(result.getRecentCreatedCount()).isEqualTo(3);
    }

    @Test
    void scheduler_실행이력이_없으면_DEGRADED() {
        ReminderHealthTracker tracker = new ReminderHealthTracker();
        ReminderHealthService service = new ReminderHealthService(tracker);

        var result = service.getReminderHealth();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.DEGRADED);
        assertThat(result.getLastRunAt()).isNull();
        assertThat(result.getLastSuccessAt()).isNull();
        assertThat(result.getNotes()).isNotEmpty();
    }

    @Test
    void scheduler_실행은_있고_성공이력은_없어도_응답은_정상생성() {
        ReminderHealthTracker tracker = new ReminderHealthTracker();
        tracker.markRunStarted();

        ReminderHealthService service = new ReminderHealthService(tracker);

        var result = service.getReminderHealth();

        assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DEGRADED);
        assertThat(result.getLastRunAt()).isNotNull();
        assertThat(result.getLastSuccessAt()).isNull();
    }
}