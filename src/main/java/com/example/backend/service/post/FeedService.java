package com.example.backend.service.post;

import com.example.backend.controller.post.dto.FeedResponse;
import com.example.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final PostRepository postRepository;

    public FeedResponse getFollowingFeed(UUID meId, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Cursor parsed = parseCursor(cursor);

        // ✅ hasNext 판별을 위해 +1로 조회 (Notification과 동일 패턴)
        int limit = pageSize + 1;

        List<FeedResponse.FeedItem> fetched = (parsed.createdAt() == null)
                ? postRepository.findFollowingFeedFirstPage(meId, limit)
                : postRepository.findFollowingFeedNextPage(meId, parsed.createdAt(), parsed.postId(), limit);

        boolean hasNext = fetched.size() > pageSize;
        List<FeedResponse.FeedItem> items = hasNext ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            FeedResponse.FeedItem last = items.get(items.size() - 1);
            nextCursor = encodeCursor(last.createdAt(), last.postId());
        }

        return new FeedResponse(items, nextCursor, hasNext);
    }

    private int normalizeSize(int size) {
        int v = size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }

    private Cursor parseCursor(String raw) {
        // ✅ Notification 스타일: 이상하면 첫 페이지로 처리
        if (raw == null || raw.isBlank()) return new Cursor(null, null);

        String[] parts = raw.split("\\|");
        if (parts.length != 2) return new Cursor(null, null);

        try {
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            UUID postId = UUID.fromString(parts[1]);
            return new Cursor(createdAt, postId);
        } catch (Exception e) {
            return new Cursor(null, null);
        }
    }

    private String encodeCursor(LocalDateTime createdAt, UUID postId) {
        return createdAt + "|" + postId;
    }

    private record Cursor(LocalDateTime createdAt, UUID postId) {}
}