package com.example.backend.repository.like;

import com.example.backend.domain.like.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<PostLike> findByPostIdAndUserId(UUID postId, UUID userId);

    // ✅ 피드에서 likedByMe를 한번에 계산하기 위한 배치 조회
    List<PostLike> findAllByUserIdAndPostIdIn(UUID userId, Collection<UUID> postIds);
}
