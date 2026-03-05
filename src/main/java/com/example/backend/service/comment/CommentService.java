package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentListResponse;
import com.example.backend.controller.comment.dto.CommentResponse;

import java.util.UUID;

public interface CommentService {
    CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request);

    /**
     * @param sort LATEST | POPULAR (null이면 LATEST)
     */
    CommentListResponse list(UUID postId, String cursor, int size, String sort);

    default CommentListResponse list(UUID postId, String cursor, int size) {
        return list(postId, cursor, size, "LATEST");
    }

    void delete(UUID commentId, UUID userId);
}
