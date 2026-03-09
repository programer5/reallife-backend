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
        LocalDateTime lastEventAt = sseHealthTracker.getLastEventSentAt();
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

        if (lastEventAt == null) {
            notes.add("아직 SSE 전송 이력이 없습니다.");
        } else {
            long minutes = Duration.between(lastEventAt, LocalDateTime.now()).toMinutes();
            notes.add("마지막 SSE 전송 후 " + minutes + "분 경과");
            if (minutes >= 30) {
                status = HealthStatus.DEGRADED;
            }
        }

        return RealtimeHealthResponse.builder()
                .status(status)
                .activeSseConnections(active)
                .lastSseEventSentAt(lastEventAt)
                .lastNotificationCreatedAt(lastNotificationAt)
                .lastMessageNotificationCreatedAt(lastMessageNotificationAt)
                .lastPinRemindNotificationCreatedAt(lastPinRemindNotificationAt)
                .notes(notes)
                .serverTime(LocalDateTime.now())
                .build();
    }
}