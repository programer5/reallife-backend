package com.example.backend.controller.notification.dto;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationListResponse(
        List<Item> items
) {
    public record Item(
            UUID id,
            NotificationType type,
            UUID refId,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static Item from(Notification n) {
            return new Item(
                    n.getId(),
                    n.getType(),
                    n.getRefId(),
                    n.getBody(),
                    n.isRead(),
                    n.getCreatedAt()
            );
        }
    }
}