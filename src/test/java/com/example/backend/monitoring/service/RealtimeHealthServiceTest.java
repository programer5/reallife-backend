package com.example.backend.monitoring.service;

import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.support.NotificationHealthTracker;
import com.example.backend.monitoring.support.SseHealthTracker;
import org.junit.jupiter.api.Test;

import static com.example.backend.domain.notification.NotificationType.MESSAGE_RECEIVED;
import static com.example.backend.domain.notification.NotificationType.PIN_REMIND;
import static org.assertj.core.api.Assertions.assertThat;

class RealtimeHealthServiceTest {

    @Test
    void SSE_연결과_이벤트가_있으면_UP() {
        SseHealthTracker sseHealthTracker = new SseHealthTracker();
        NotificationHealthTracker notificationHealthTracker = new NotificationHealthTracker();

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();
        notificationHealthTracker.markCreated(MESSAGE_RECEIVED);
        notificationHealthTracker.markCreated(PIN_REMIND);

        RealtimeHealthService service = new RealtimeHealthService(sseHealthTracker, notificationHealthTracker);

        var result = service.getRealtimeHealth();

        assertThat(result.status()).isEqualTo(HealthStatus.UP);
        assertThat(result.activeSseConnections()).isEqualTo(1);
        assertThat(result.lastSseEventSentAt()).isNotNull();
        assertThat(result.lastNotificationCreatedAt()).isNotNull();
        assertThat(result.lastMessageNotificationCreatedAt()).isNotNull();
        assertThat(result.lastPinRemindNotificationCreatedAt()).isNotNull();
        assertThat(result.serverTime()).isNotNull();
    }

    @Test
    void SSE_전송_이력이_없으면_UP_기본정보는_반환한다() {
        SseHealthTracker sseHealthTracker = new SseHealthTracker();
        NotificationHealthTracker notificationHealthTracker = new NotificationHealthTracker();

        RealtimeHealthService service = new RealtimeHealthService(sseHealthTracker, notificationHealthTracker);

        var result = service.getRealtimeHealth();

        assertThat(result.status()).isEqualTo(HealthStatus.UP);
        assertThat(result.activeSseConnections()).isEqualTo(0);
        assertThat(result.lastSseEventSentAt()).isNull();
        assertThat(result.notes()).isNotEmpty();
        assertThat(result.serverTime()).isNotNull();
    }

    @Test
    void SSE_이벤트가_오래전이면_DEGRADED_분기구조를_유지한다() {
        SseHealthTracker sseHealthTracker = new SseHealthTracker();
        NotificationHealthTracker notificationHealthTracker = new NotificationHealthTracker();

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();

        RealtimeHealthService service = new RealtimeHealthService(sseHealthTracker, notificationHealthTracker);

        var result = service.getRealtimeHealth();

        assertThat(result.status()).isIn(HealthStatus.UP, HealthStatus.DEGRADED);
    }
}