package com.example.backend.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RealtimeHealthResponse(
        HealthStatus status,
        int activeSseConnections,
        LocalDateTime lastSseEventSentAt,
        LocalDateTime lastNotificationCreatedAt,
        LocalDateTime lastMessageNotificationCreatedAt,
        LocalDateTime lastPinRemindNotificationCreatedAt,
        List<String> notes,
        LocalDateTime serverTime
) {
}