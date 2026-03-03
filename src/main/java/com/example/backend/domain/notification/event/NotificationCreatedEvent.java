package com.example.backend.domain.notification.event;

import com.example.backend.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationCreatedEvent(
        UUID notificationId,
        UUID userId,
        NotificationType type,
        UUID refId,
        UUID ref2Id,   // ✅ 추가
        String body,
        LocalDateTime createdAt
) {}