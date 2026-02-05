package com.example.backend.repository.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepositoryCustom {

    int markAllAsRead(UUID userId, LocalDateTime now);

    int markAsReadIfUnread(UUID notificationId, UUID userId, LocalDateTime now);

    // ✅ 추가: 읽은 알림만 soft delete
    int softDeleteRead(UUID userId);
}