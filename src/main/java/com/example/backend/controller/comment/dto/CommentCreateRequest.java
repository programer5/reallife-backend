package com.example.backend.controller.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * content: 댓글 본문
 * parentCommentId: 1-depth 답글용(없으면 루트 댓글)
 */
public record CommentCreateRequest(
        @NotBlank @Size(max = 2000) String content,
        UUID parentCommentId
) {}
