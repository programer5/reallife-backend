package com.example.backend.repository.post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ✅ 피드는 정렬/가시성 조건이 중요해서 QueryDSL로 "ID만" 먼저 뽑고,
 * 이후 Post 엔티티를 fetch join으로 한 번 더 가져옵니다 (이미지 + 카운트 포함).
 */
public interface PostQueryRepository {

    List<UUID> findFollowingFeedIdsFirstPage(UUID meId, int size);

    List<UUID> findFollowingFeedIdsNextPage(
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int size
    );
}
