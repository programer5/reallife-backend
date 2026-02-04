package com.example.backend.controller.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationListResponse(
        List<Item> items
) {
    public record Item(
            UUID notificationId,
            String type,
            UUID refId,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}
}