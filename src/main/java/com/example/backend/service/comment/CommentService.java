package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentResponse;

import java.util.UUID;

public interface CommentService {
    CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request);
    void delete(UUID commentId, UUID userId);
}