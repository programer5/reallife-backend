package com.example.backend.controller.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedResponse(
        List<FeedItem> items,
        Cursor nextCursor,
        boolean hasNext
) {
    public record FeedItem(
            UUID postId,
            UUID authorId,
            String authorHandle,
            String authorName,
            String content,
            List<String> imageUrls,
            String visibility,
            LocalDateTime createdAt
    ) {}

    public record Cursor(
            LocalDateTime cursorCreatedAt,
            UUID cursorPostId
    ) {}
}
