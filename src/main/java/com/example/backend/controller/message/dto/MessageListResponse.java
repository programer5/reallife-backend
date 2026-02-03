package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageListResponse(
        List<Item> items,
        Cursor nextCursor,
        boolean hasNext
) {
    public record Item(
            UUID messageId,
            UUID senderId,
            String content,
            LocalDateTime createdAt,
            List<Attachment> attachments
    ) {}

    public record Attachment(
            UUID attachmentId,
            String originalName,
            String mimeType,
            long sizeBytes,
            String downloadUrl
    ) {}

    public record Cursor(
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId
    ) {}
}