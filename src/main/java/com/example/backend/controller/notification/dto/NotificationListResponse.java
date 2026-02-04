package com.example.backend.controller.notification.dto;

import com.example.backend.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationListResponse(
        List<Item> items,
        boolean hasUnread
) {

    public static NotificationListResponse from(
            List<Notification> notifications,
            boolean hasUnread
    ) {
        List<Item> items = notifications.stream()
                .map(Item::from)
                .toList();

        return new NotificationListResponse(items, hasUnread);
    }

    public record Item(
            UUID id,
            String type,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static Item from(Notification notification) {
            return new Item(
                    notification.getId(),
                    notification.getType().name(),
                    notification.getBody(),
                    notification.isRead(),
                    notification.getCreatedAt()
            );
        }
    }
}