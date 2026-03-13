
package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageCapsuleListResponse(
        UUID conversationId,
        List<Item> items
) {
    public record Item(
            UUID capsuleId,
            UUID messageId,
            UUID creatorId,
            String title,
            LocalDateTime unlockAt,
            LocalDateTime openedAt,
            boolean opened
    ) {}
}
