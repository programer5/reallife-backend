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

        assertThat(result.status()).isEqualTo(HealthStatus.UP);
        assertThat(result.schedulerEnabled()).isTrue();
        assertThat(result.lastRunAt()).isNotNull();
        assertThat(result.lastSuccessAt()).isNotNull();
        assertThat(result.recentCreatedCount()).isEqualTo(3);
        assertThat(result.serverTime()).isNotNull();
    }

    @Test
    void 실행이력이_없으면_DEGRADED() {
        ReminderHealthTracker tracker = new ReminderHealthTracker();

        ReminderHealthService service = new ReminderHealthService(tracker);

        var result = service.getReminderHealth();

        assertThat(result.status()).isEqualTo(HealthStatus.DEGRADED);
        assertThat(result.schedulerEnabled()).isTrue();
        assertThat(result.lastRunAt()).isNull();
        assertThat(result.lastSuccessAt()).isNull();
        assertThat(result.notes()).isNotEmpty();
        assertThat(result.serverTime()).isNotNull();
    }
}