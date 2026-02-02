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
        int pageSize = Math.min(Math.max(size, 1), 50);

        LocalDateTime cursorCreatedAt = null;
        UUID cursorPostId = null;

        if (cursor != null && !cursor.isBlank()) {
            Cursor decoded = Cursor.decode(cursor);
            cursorCreatedAt = decoded.createdAt();
            cursorPostId = decoded.postId();
        }

        List<FeedResponse.FeedItem> fetched = (cursorCreatedAt == null)
                ? postRepository.findFollowingFeedFirstPage(meId, pageSize)
                : postRepository.findFollowingFeedNextPage(meId, cursorCreatedAt, cursorPostId, pageSize);

        boolean hasNext = fetched.size() > pageSize;

        List<FeedResponse.FeedItem> items = hasNext
                ? fetched.subList(0, pageSize)
                : fetched;

        FeedResponse.Cursor nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            FeedResponse.FeedItem last = items.get(items.size() - 1);
            nextCursor = new FeedResponse.Cursor(last.createdAt(), last.postId());
        }

        return new FeedResponse(items, nextCursor, hasNext);
    }

    // 커서는 문자열로 유지 (요청에서 그대로 넣기 편함)
    // 형식: "2026-02-02T12:00:00|uuid"
    private record Cursor(LocalDateTime createdAt, UUID postId) {
        static Cursor decode(String raw) {
            try {
                String[] parts = raw.split("\\|");
                return new Cursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (Exception e) {
                throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다. 예) 2026-02-02T12:00:00|uuid");
            }
        }

        static String encode(LocalDateTime createdAt, UUID postId) {
            return createdAt + "|" + postId;
        }
    }
}