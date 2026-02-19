package com.example.backend.service.post;

import com.example.backend.controller.post.dto.FeedResponse;
import com.example.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final PostRepository postRepository;

    public FeedResponse getFollowingFeed(UUID meId, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Cursor parsed = parseCursor(cursor);

        // ✅ hasNext 판별을 위해 +1로 조회
        int limit = pageSize + 1;

        List<FeedResponse.FeedItem> fetched = (parsed.createdAt() == null)
                ? postRepository.findFollowingFeedFirstPage(meId, limit)
                : postRepository.findFollowingFeedNextPage(meId, parsed.createdAt(), parsed.postId(), limit);

        boolean hasNext = fetched.size() > pageSize;
        List<FeedResponse.FeedItem> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        // ✅ imageUrls 보강 (feed query가 imageUrls를 빈 리스트로 내리므로 여기서 채워준다)
        List<FeedResponse.FeedItem> items = enrichImageUrls(page);

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            FeedResponse.FeedItem last = items.get(items.size() - 1);
            nextCursor = encodeCursor(last.createdAt(), last.postId());
        }

        return new FeedResponse(items, nextCursor, hasNext);
    }

    private List<FeedResponse.FeedItem> enrichImageUrls(List<FeedResponse.FeedItem> items) {
        if (items == null || items.isEmpty()) return items;

        List<UUID> ids = items.stream().map(FeedResponse.FeedItem::postId).toList();

        // fetch join으로 images까지 한 번에 로드
        var posts = postRepository.findAllWithImagesByIdIn(ids);

        Map<UUID, List<String>> map = posts.stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getImages().stream()
                                .sorted(Comparator.comparingInt(img -> img.getSortOrder()))
                                .map(img -> img.getImageUrl())
                                .toList()
                ));

        return items.stream()
                .map(i -> new FeedResponse.FeedItem(
                        i.postId(),
                        i.authorId(),
                        i.authorHandle(),
                        i.authorName(),
                        i.content(),
                        map.getOrDefault(i.postId(), List.of()),
                        i.visibility(),
                        i.createdAt()
                ))
                .toList();
    }

    private int normalizeSize(int size) {
        int v = size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }

    private Cursor parseCursor(String raw) {
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
