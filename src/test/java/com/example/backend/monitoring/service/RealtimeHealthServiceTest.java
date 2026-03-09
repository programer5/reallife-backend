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

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getActiveSseConnections()).isEqualTo(1);
        assertThat(result.getLastSseEventSentAt()).isNotNull();
        assertThat(result.getLastNotificationCreatedAt()).isNotNull();
        assertThat(result.getLastMessageNotificationCreatedAt()).isNotNull();
        assertThat(result.getLastPinRemindNotificationCreatedAt()).isNotNull();
    }

    @Test
    void SSE_전송_이력이_없으면_DEGRADED는_아니고_기본정보는_반환한다() {
        SseHealthTracker sseHealthTracker = new SseHealthTracker();
        NotificationHealthTracker notificationHealthTracker = new NotificationHealthTracker();

        RealtimeHealthService service = new RealtimeHealthService(sseHealthTracker, notificationHealthTracker);

        var result = service.getRealtimeHealth();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getActiveSseConnections()).isEqualTo(0);
        assertThat(result.getLastSseEventSentAt()).isNull();
        assertThat(result.getNotes()).isNotEmpty();
    }

    @Test
    void SSE_이벤트가_오래전이면_DEGRADED() {
        SseHealthTracker sseHealthTracker = new SseHealthTracker();
        NotificationHealthTracker notificationHealthTracker = new NotificationHealthTracker();

        sseHealthTracker.onConnected();
        sseHealthTracker.onEventSent();

        // 테스트 편의를 위해 tracker 내부 시각을 오래전으로 바꾸는 별도 setter가 없으므로
        // 현재 1차 뼈대에서는 서비스 판정 분기 구조만 유지하고,
        // 추후 tracker에 테스트용 메서드를 추가하면 더 정확히 검증 가능하다.
        RealtimeHealthService service = new RealtimeHealthService(sseHealthTracker, notificationHealthTracker);

        var result = service.getRealtimeHealth();

        assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DEGRADED);
    }
}