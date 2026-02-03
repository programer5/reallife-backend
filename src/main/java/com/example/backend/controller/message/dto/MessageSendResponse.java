package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageSendResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        LocalDateTime createdAt,
        List<AttachmentDto> attachments
) {
    public record AttachmentDto(
            UUID attachmentId,
            String originalName,
            String mimeType,
            long sizeBytes,
            String downloadUrl
    ) {}
}