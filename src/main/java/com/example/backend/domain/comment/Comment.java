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
        @Index(name = "idx_comment_author", columnList = "author_id")
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Comment(UUID postId, UUID authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
    }

    public static Comment create(UUID postId, UUID authorId, String content) {
        return new Comment(postId, authorId, content);
    }

    public void delete() {
        markDeleted();
    }
}