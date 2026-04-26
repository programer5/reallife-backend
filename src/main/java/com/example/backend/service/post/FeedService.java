package com.example.backend.service.post;

import com.example.backend.common.MediaPayloads;
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
        FeedResponse response = buildFeedResponse(meId, pageIds, null, hasNext);

        String nextCursor = null;
        if (hasNext && !response.items().isEmpty()) {
            FeedResponse.FeedItem last = response.items().get(response.items().size() - 1);
            nextCursor = encodeCursor(last.createdAt(), last.postId());
        }
        return new FeedResponse(response.items(), nextCursor, hasNext);
    }

    public FeedResponse getNearbyFeed(UUID meId, double lat, double lng, int size) {
        int pageSize = normalizeSize(size);
        if (!isValidCoordinate(lat, lng)) {
            return new FeedResponse(List.of(), null, false);
        }

        List<UUID> ids = postRepository.findNearbyFeedIds(meId, Math.max(pageSize * 4, pageSize));
        FeedResponse response = buildFeedResponse(meId, ids, new Point(lat, lng), false);
        List<FeedResponse.FeedItem> sorted = response.items().stream()
                .sorted(Comparator
                        .comparing((FeedResponse.FeedItem item) -> Optional.ofNullable(item.distanceKm()).orElse(Double.MAX_VALUE))
                        .thenComparing(FeedResponse.FeedItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(pageSize)
                .toList();
        return new FeedResponse(sorted, null, false);
    }

    private FeedResponse buildFeedResponse(UUID meId, List<UUID> pageIds, Point origin, boolean hasNext) {
        if (pageIds == null || pageIds.isEmpty()) return new FeedResponse(List.of(), null, hasNext);

        List<Post> posts = postRepository.findAllWithImagesByIdIn(pageIds);
        Map<UUID, Post> byId = posts.stream().collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));
        List<Post> ordered = pageIds.stream().map(byId::get).filter(Objects::nonNull).toList();

        Set<UUID> authorIds = ordered.stream().map(Post::getAuthorId).collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        Set<UUID> liked = postLikeRepository.findAllByUserIdAndPostIdIn(meId, pageIds)
                .stream()
                .map(pl -> pl.getPostId())
                .collect(Collectors.toSet());

        List<FeedResponse.FeedItem> items = ordered.stream()
                .map(p -> toFeedItem(p, users.get(p.getAuthorId()), liked.contains(p.getId()), origin))
                .toList();

        return new FeedResponse(items, null, hasNext);
    }

    private FeedResponse.FeedItem toFeedItem(Post p, User u, boolean likedByMe, Point origin) {
        String handle = (u != null) ? u.getHandle() : null;
        String name = (u != null) ? u.getName() : null;
        Double distanceKm = distanceKm(origin, p.getLatitude(), p.getLongitude());

        return new FeedResponse.FeedItem(
                p.getId(),
                p.getAuthorId(),
                handle,
                name,
                p.getContent(),
                p.getImages().stream()
                        .filter(img -> String.valueOf(img.getMediaType()).equals("IMAGE"))
                        .map(PostImage::getImageUrl)
                        .toList(),
                p.getImages().stream()
                        .map(img -> new FeedResponse.MediaItem(
                                img.getFile() != null ? img.getFile().getId() : null,
                                img.getMediaType().name(),
                                img.getImageUrl(),
                                img.getImageUrl(),
                                MediaPayloads.previewUrl(img.getMediaType().name(), img.getImageUrl()),
                                img.getThumbnailUrl(),
                                MediaPayloads.streamingUrl(img.getMediaType().name(), img.getImageUrl()),
                                img.getFile() != null ? img.getFile().getOriginalFilename() : null,
                                img.getContentType(),
                                img.getFile() != null ? img.getFile().getSize() : 0L
                        ))
                        .toList(),
                p.getVisibility().name(),
                p.getCreatedAt(),
                p.getLikeCount(),
                p.getCommentCount(),
                likedByMe,
                p.getLatitude(),
                p.getLongitude(),
                p.getPlaceName(),
                distanceKm
        );
    }

    private int normalizeSize(int size) {
        int v = size;
        if (v < 1) v = 1;
        if (v > 50) v = 50;
        return v;
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private Double distanceKm(Point origin, Double lat, Double lng) {
        if (origin == null || lat == null || lng == null) return null;
        double r = 6371.0;
        double dLat = Math.toRadians(lat - origin.lat());
        double dLng = Math.toRadians(lng - origin.lng());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(origin.lat())) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double value = r * c;
        return Math.round(value * 10.0) / 10.0;
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
    private record Point(double lat, double lng) {}
}
