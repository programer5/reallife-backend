package com.example.backend.repository.like;

import com.example.backend.domain.like.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);
    Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);
    void deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}
