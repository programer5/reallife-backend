package com.example.backend.controller.me.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MyCapsuleListResponse(
        List<Item> items
) {
    public record Item(
            UUID capsuleId,
            UUID conversationId,
            UUID messageId,
            String title,
            LocalDateTime unlockAt,
            boolean opened
    ) {}
}
