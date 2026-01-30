package com.example.backend.repository.post;

import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostVisibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PostQueryRepository {
    List<Post> findFeedFirstPage(PostVisibility visibility, int size);
    List<Post> findFeedNextPage(PostVisibility visibility, LocalDateTime cursorCreatedAt, UUID cursorId, int size);
}
