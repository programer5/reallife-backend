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
            String type,
            String content,
            String metadataJson,
            UUID sessionId,
            LocalDateTime createdAt,
            LocalDateTime editedAt,
            List<Attachment> attachments,
            List<PinCandidateResponse> pinCandidates
    ) {}

    public record Attachment(
            UUID fileId,
            String mediaType,
            String url,
            String downloadUrl,
            String previewUrl,
            String thumbnailUrl,
            String streamingUrl,
            String originalFilename,
            String contentType,
            long size
    ) {}
}
