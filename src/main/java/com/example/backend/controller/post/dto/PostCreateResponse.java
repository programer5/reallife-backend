package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostCreateResponse(
        UUID postId,
        UUID authorId,
        String authorHandle,
        String authorName,
        String content,
        List<String> imageUrls,
        List<MediaItem> mediaItems,
        PostVisibility visibility,
        LocalDateTime createdAt,
        long likeCount,
        long commentCount,
        boolean likedByMe
) {
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
