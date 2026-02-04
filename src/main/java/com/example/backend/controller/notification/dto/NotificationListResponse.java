package com.example.backend.controller.notification.dto;

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
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}
}