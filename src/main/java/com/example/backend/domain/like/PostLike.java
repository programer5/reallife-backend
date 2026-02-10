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
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_user_like", columnNames = {"post_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_post_id", columnList = "post_id"),
                @Index(name = "idx_post_like_user_id", columnList = "user_id")
        }
)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private PostLike(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }

    public static PostLike create(UUID postId, UUID userId) {
        return new PostLike(postId, userId);
    }
}
