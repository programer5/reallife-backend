package com.example.backend.repository.comment;

import com.example.backend.domain.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
           select c
           from Comment c
           where c.post.id = :postId
             and c.deleted = false
           order by c.createdAt desc
           """)
    Page<Comment> findActiveByPostId(UUID postId, Pageable pageable);
}