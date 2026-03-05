package com.example.backend.service.like;

import java.util.UUID;

public interface CommentLikeService {
    void like(UUID commentId, UUID userId);
    void unlike(UUID commentId, UUID userId);
}
