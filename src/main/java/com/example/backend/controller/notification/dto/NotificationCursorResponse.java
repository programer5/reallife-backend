package com.example.backend.controller.notification.dto;

import com.example.backend.domain.notification.Notification;

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
            UUID conversationId,   // ✅ 추가 (PIN_*일 때만 채움, 나머진 null)
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}

    public static NotificationCursorResponse of(
            List<Notification> page,
            boolean hasNext,
            boolean hasUnread,
            java.util.Map<UUID, UUID> pinCidMap
    ) {
        List<Item> items = page.stream()
                .map(n -> new Item(
                        n.getId(),
                        n.getType().name(),
                        n.getRefId(),
                        pinCidMap.get(n.getRefId()), // ✅ PIN_*면 채워지고, 아니면 null
                        n.getBody(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString() + "|" + last.getId();
        }
        return new NotificationCursorResponse(items, nextCursor, hasNext, hasUnread);
    }
}