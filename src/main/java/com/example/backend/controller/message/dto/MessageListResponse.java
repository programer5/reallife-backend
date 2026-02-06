package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageListResponse(
        List<Item> items,
        String nextCursor,
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
            UUID fileId,
            String url,
            String originalFilename,
            String contentType,
            long size
    ) {}
}