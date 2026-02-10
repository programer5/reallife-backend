package com.example.backend.controller.comment.dto;

import com.example.backend.domain.comment.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID commentId,
        UUID postId,
        UUID userId,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),     // ✅ 여기!
                comment.getAuthorId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}