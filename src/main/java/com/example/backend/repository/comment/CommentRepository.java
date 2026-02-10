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

    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt
        )
        from Comment c
        join User u on u.id = c.authorId
        where c.postId = :postId
          and c.deleted = false
        order by c.createdAt desc, c.id desc
    """)
    List<CommentListItem> findFirstPage(UUID postId, Pageable pageable);

    @Query("""
        select new com.example.backend.controller.comment.dto.CommentListItem(
            c.id,
            c.authorId,
            u.handle,
            u.name,
            c.content,
            c.createdAt
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
    List<CommentListItem> findNextPage(
            UUID postId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            Pageable pageable
    );
}