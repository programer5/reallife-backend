package com.example.backend.service.post;

import com.example.backend.controller.post.dto.FeedResponse;
import com.example.backend.domain.post.Post;
import com.example.backend.domain.post.PostImage;
import com.example.backend.domain.user.User;
import com.example.backend.repository.like.PostLikeRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    public FeedResponse getFollowingFeed(UUID meId, String cursor, int size) {
        int pageSize = normalizeSize(size);
        Cursor parsed = parseCursor(cursor);

        int limit = pageSize + 1;

        List<UUID> ids = (parsed.createdAt() == null)
                ? postRepository.findFollowingFeedIdsFirstPage(meId, limit)
                : postRepository.findFollowingFeedIdsNextPage(meId, parsed.createdAt(), parsed.postId(), limit);

        boolean hasNext = ids.size() > pageSize;
        List<UUID> pageIds = hasNext ? ids.subList(0, pageSize) : ids;

        if (pageIds.isEmpty()) return new FeedResponse(List.of(), null, false);

        // ✅ Post + images 한 번에 로딩
        List<Post> posts = postRepository.findAllWithImagesByIdIn(pageIds);

        // DB에서 IN 조회는 정렬 보장 X → feed ids 순서대로 다시 정렬
        Map<UUID, Post> byId = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity(), (a,b)->a));
        List<Post> ordered = pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();

        // ✅ author 정보 배치 조회
        Set<UUID> authorIds = ordered.stream().map(Post::getAuthorId).collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a,b)->a));

        // ✅ likedByMe 배치 조회
        Set<UUID> liked = postLikeRepository.findAllByUserIdAndPostIdIn(meId, pageIds)
                .stream()
                .map(pl -> pl.getPostId())
                .collect(Collectors.toSet());

        List<FeedResponse.FeedItem> items = ordered.stream()
                .map(p -> {
                    User u = users.get(p.getAuthorId());
                    String handle = (u != null) ? u.getHandle() : null;
                    String name = (u != null) ? u.getName() : null;

                    return new FeedResponse.FeedItem(
                            p.getId(),
                            p.getAuthorId(),
                            handle,
                            name,
                            p.getContent(),
                            p.getImages().stream().map(PostImage::getImageUrl).toList(),
                            p.getVisibility().name(),
                            p.getCreatedAt(),
                            p.getLikeCount(),
                            p.getCommentCount(),
                            liked.contains(p.getId())
                    );
                })
                .toList();

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
