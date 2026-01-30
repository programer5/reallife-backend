package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostFeedItem(
        UUID postId,
        UUID authorId,
        String content,
        List<String> imageUrls,
        PostVisibility visibility,
        LocalDateTime createdAt
) {
}
