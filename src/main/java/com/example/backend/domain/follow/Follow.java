package com.example.backend.domain.follow;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_follower_following", columnNames = {"follower_id", "following_id"})
        },
        indexes = {
                @Index(name = "idx_follower_id", columnList = "follower_id"),
                @Index(name = "idx_following_id", columnList = "following_id")
        }
)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    private Follow(UUID followerId, UUID followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public static Follow create(UUID followerId, UUID followingId) {
        return new Follow(followerId, followingId);
    }
}
