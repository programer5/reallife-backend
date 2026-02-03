package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageSendResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        List<FileItem> attachments,
        LocalDateTime createdAt
) {
    public record FileItem(UUID fileId, String url, String originalFilename, String contentType, long size) {}
}