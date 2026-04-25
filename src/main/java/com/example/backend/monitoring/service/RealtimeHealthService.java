package com.example.backend.monitoring.service;

import com.example.backend.domain.notification.NotificationType;
import com.example.backend.monitoring.dto.HealthStatus;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.support.NotificationHealthTracker;
import com.example.backend.monitoring.support.SseHealthTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeHealthService {

    private final SseHealthTracker sseHealthTracker;
    private final NotificationHealthTracker notificationHealthTracker;

    public RealtimeHealthResponse getRealtimeHealth() {
        int active = sseHealthTracker.getActiveConnections();
        LocalDateTime lastRegisteredAt = sseHealthTracker.getLastRegisteredAt();
        LocalDateTime lastDisconnectedAt = sseHealthTracker.getLastDisconnectedAt();
        LocalDateTime lastEventAt = sseHealthTracker.getLastEventSentAt();
        LocalDateTime lastFailureAt = sseHealthTracker.getLastFailureAt();
        String lastFailureMessage = sseHealthTracker.getLastFailureMessage();
        long failureCount = sseHealthTracker.getFailureCount();
        LocalDateTime lastNotificationAt = notificationHealthTracker.getLastCreatedAt();
        LocalDateTime lastMessageNotificationAt =
                notificationHealthTracker.getLastCreatedAt(NotificationType.MESSAGE_RECEIVED);
        LocalDateTime lastPinRemindNotificationAt =
                notificationHealthTracker.getLastCreatedAt(NotificationType.PIN_REMIND);

        List<String> notes = new ArrayList<>();
        HealthStatus status = HealthStatus.UP;

        if (active == 0) {
            notes.add("현재 활성 SSE 연결이 없습니다.");
        } else {
            notes.add("활성 SSE 연결이 존재합니다.");
        }

        if (lastRegisteredAt == null) {
            notes.add("아직 SSE 연결 등록 이력이 없습니다.");
        } else {
            notes.add("최근 SSE 연결 등록 이력이 있습니다.");
        }

        if (lastEventAt == null) {
            notes.add("아직 SSE 전송 이력이 없습니다.");
        } else {
            long minutes = Duration.between(lastEventAt, LocalDateTime.now()).toMinutes();
            notes.add("마지막 SSE 전송 후 " + minutes + "분 경과");
            if (minutes >= 30) {
                status = HealthStatus.DEGRADED;
            }
        }

        if (lastFailureAt != null) {
            status = HealthStatus.DOWN;
            notes.add("마지막 SSE 처리 중 오류가 발생했습니다.");
            if (lastFailureMessage != null && !lastFailureMessage.isBlank()) {
                notes.add("lastSseFailureMessage=" + lastFailureMessage);
            }
        }

        return new RealtimeHealthResponse(
                status,
                active,
                lastRegisteredAt,
                lastDisconnectedAt,
                lastEventAt,
                lastFailureAt,
                lastFailureMessage,
                failureCount,
                lastNotificationAt,
                lastMessageNotificationAt,
                lastPinRemindNotificationAt,
                notes,
                LocalDateTime.now()
        );
    }
}
