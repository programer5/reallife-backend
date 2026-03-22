package com.example.backend.controller.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedResponse(
        List<FeedItem> items,
        String nextCursor,
        boolean hasNext
) {
    public record FeedItem(
            UUID postId,
            UUID authorId,
            String authorHandle,
            String authorName,
            String content,
            List<String> imageUrls,
            List<MediaItem> mediaItems,
            String visibility,
            LocalDateTime createdAt,
            long likeCount,
            long commentCount,
            boolean likedByMe
    ) {}

    public record MediaItem(
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
