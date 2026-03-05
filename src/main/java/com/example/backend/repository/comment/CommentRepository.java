package com.example.backend.repository.comment;

import com.example.backend.controller.comment.dto.CommentListItem;
import com.example.backend.domain.comment.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    boolean existsByIdAndPostIdAndDeletedFalse(UUID id, UUID postId);

    // ===== LATEST =====
    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt,
            c.parentCommentId,
            c.likeCount
        )
        from Comment c
        join User u on u.id = c.authorId
        where c.postId = :postId
          and c.deleted = false
        order by c.createdAt desc, c.id desc
    """)
    List<CommentListItem> findFirstPageLatest(UUID postId, Pageable pageable);

    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt,
            c.parentCommentId,
            c.likeCount
        )
        from Comment c
        join User u on u.id = c.authorId
        where c.postId = :postId
          and c.deleted = false
          and (
                c.createdAt < :cursorCreatedAt
             or (c.createdAt = :cursorCreatedAt and c.id < :cursorId)
          )
        order by c.createdAt desc, c.id desc
    """)
    List<CommentListItem> findNextPageLatest(
            UUID postId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            Pageable pageable
    );

    // ===== POPULAR =====
    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt,
            c.parentCommentId,
            c.likeCount
        )
        from Comment c
        join User u on u.id = c.authorId
        where c.postId = :postId
          and c.deleted = false
        order by c.likeCount desc, c.createdAt desc, c.id desc
    """)
    List<CommentListItem> findFirstPagePopular(UUID postId, Pageable pageable);

    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt,
            c.parentCommentId,
            c.likeCount
        )
        from Comment c
        join User u on u.id = c.authorId
        where c.postId = :postId
          and c.deleted = false
          and (
                c.likeCount < :cursorLikeCount
             or (c.likeCount = :cursorLikeCount and c.createdAt < :cursorCreatedAt)
             or (c.likeCount = :cursorLikeCount and c.createdAt = :cursorCreatedAt and c.id < :cursorId)
          )
        order by c.likeCount desc, c.createdAt desc, c.id desc
    """)
    List<CommentListItem> findNextPagePopular(
            UUID postId,
            long cursorLikeCount,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            Pageable pageable
    );
}
