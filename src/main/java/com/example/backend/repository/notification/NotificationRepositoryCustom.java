package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {

    int markAllAsRead(UUID userId, LocalDateTime now);

    int markAsReadIfUnread(UUID notificationId, UUID userId, LocalDateTime now);

    int hardDeleteRead(UUID userId);

    // ✅ 커서 기반 조회(최신순): createdAt DESC, id DESC
    List<Notification> findMyNotificationsByCursor(
            UUID userId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int limit
    );
}