package com.example.backend.domain.like;

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
@Table(
        name = "comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comment_user_like", columnNames = {"comment_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_comment_like_comment_id", columnList = "comment_id"),
                @Index(name = "idx_comment_like_user_id", columnList = "user_id")
        }
)
public class CommentLike extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private CommentLike(UUID commentId, UUID userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    public static CommentLike create(UUID commentId, UUID userId) {
        return new CommentLike(commentId, userId);
    }
}
