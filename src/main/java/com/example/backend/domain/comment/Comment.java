package com.example.backend.domain.comment;

import com.example.backend.domain.BaseEntity;
import com.example.backend.domain.post.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_post_created", columnList = "post_id, created_at"),
                @Index(name = "idx_comment_author", columnList = "author_id")
        }
)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Comment(Post post, UUID authorId, String content) {
        this.post = post;
        this.authorId = authorId;
        this.content = content;
    }

    public static Comment create(Post post, UUID authorId, String content) {
        return new Comment(post, authorId, content);
    }

    public void delete() {
        markDeleted(); // BaseEntity soft-delete
    }
}