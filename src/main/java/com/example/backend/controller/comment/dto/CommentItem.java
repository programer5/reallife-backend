package com.example.backend.controller.comment.dto;

import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentItem(
        UUID commentId,
        UUID userId,
        String handle,
        String name,
        String content,
        LocalDateTime createdAt
) {
    public static CommentItem from(Comment c, User u) {
        return new CommentItem(
                c.getId(),
                c.getAuthorId(),
                u.getHandle(),
                u.getName(),
                c.getContent(),
                c.getCreatedAt()
        );
    }
}