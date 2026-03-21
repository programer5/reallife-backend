package com.example.backend.controller.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationCursorResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext,
        boolean hasUnread
) {
    public record Item(
            UUID id,
            String type,
            UUID refId,
            UUID ref2Id,
            UUID conversationId,
            String category,
            int priorityScore,
            String targetPath,
            String targetLabel,
            String actionHint,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}
}
