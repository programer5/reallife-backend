package com.example.backend.repository.follow;

import com.example.backend.domain.follow.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowingIdAndDeletedFalse(UUID followerId, UUID followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    List<Follow> findAllByFollowerIdAndDeletedFalse(UUID followerId);

    // ✅ 추가: 팔로워 수 / 팔로잉 수
    long countByFollowingIdAndDeletedFalse(UUID followingId); // 나를 팔로우하는 수
    long countByFollowerIdAndDeletedFalse(UUID followerId);   // 내가 팔로우하는 수

    @Query("""
        select f.followingId
        from Follow f
        where f.followerId = :meId
          and f.deleted = false
          and f.followingId in :targetIds
    """)
    List<UUID> findFollowingIds(
            @Param("meId") UUID meId,
            @Param("targetIds") List<UUID> targetIds
    );
}