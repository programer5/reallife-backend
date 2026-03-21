package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {

    int markAllAsRead(UUID userId, LocalDateTime now);

    int markAsReadIfUnread(UUID notificationId, UUID userId, LocalDateTime now);

    int hardDeleteRead(UUID userId);

    List<Notification> findMyNotificationsByCursor(
            UUID userId,
            Integer cursorPriorityScore,
            LocalDateTime cursorCreatedAt,
            int limit
    );
}
