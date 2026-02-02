package com.example.backend.repository.post;

import com.example.backend.controller.post.dto.FeedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PostQueryRepository {

    List<FeedResponse.FeedItem> findFollowingFeedFirstPage(UUID meId, int size);

    List<FeedResponse.FeedItem> findFollowingFeedNextPage(
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorId,
            int size
    );
}