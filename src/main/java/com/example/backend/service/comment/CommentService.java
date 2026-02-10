package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentListResponse;
import com.example.backend.controller.comment.dto.CommentResponse;

import java.util.UUID;

public interface CommentService {
    CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request);
    CommentListResponse list(UUID postId, String cursor, int size);
    void delete(UUID commentId, UUID userId);
}