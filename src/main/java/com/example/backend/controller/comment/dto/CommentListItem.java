package com.example.backend.controller.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentListItem(
        UUID commentId,
        UUID userId,
        String handle,
        String name,
        String content,
        LocalDateTime createdAt
) {}