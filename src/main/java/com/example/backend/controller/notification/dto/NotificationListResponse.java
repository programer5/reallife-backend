package com.example.backend.controller.notification.dto;

import com.example.backend.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationListResponse(
        List<Item> items,
        boolean hasUnread
) {
    public record Item(
            UUID id,
            String type,
            UUID refId,          // ✅ 추가
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}

    public static NotificationListResponse from(List<Notification> notifications, boolean hasUnread) {
        List<Item> items = notifications.stream()
                .map(n -> new Item(
                        n.getId(),
                        n.getType().name(),
                        n.getRefId(),     // ✅ 추가
                        n.getBody(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .toList();

        return new NotificationListResponse(items, hasUnread);
    }
}