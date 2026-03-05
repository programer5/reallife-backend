package com.example.backend.domain.comment;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_post_created", columnList = "post_id, created_at"),
        @Index(name = "idx_comment_post_parent_created", columnList = "post_id, parent_comment_id, created_at"),
        @Index(name = "idx_comment_author", columnList = "author_id"),
        @Index(name = "idx_comment_post_like_created", columnList = "post_id, like_count, created_at")
})
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId; // 1-depth reply 지원(없으면 root)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    private Comment(UUID postId, UUID authorId, UUID parentCommentId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.likeCount = 0L;
    }

    public static Comment create(UUID postId, UUID authorId, String content) {
        return new Comment(postId, authorId, null, content);
    }

    public static Comment create(UUID postId, UUID authorId, UUID parentCommentId, String content) {
        return new Comment(postId, authorId, parentCommentId, content);
    }

    public void delete() {
        markDeleted();
    }

    public void increaseLikeCount() {
        this.likeCount += 1L;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0L) this.likeCount -= 1L;
    }
}
