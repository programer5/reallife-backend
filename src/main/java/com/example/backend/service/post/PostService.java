package com.example.backend.service.post;

import com.example.backend.controller.post.dto.PostCreateRequest;
import com.example.backend.controller.post.dto.PostCreateResponse;
import com.example.backend.controller.post.dto.PostFeedResponse;
import com.example.backend.domain.post.Post;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.security.ContentSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public PostCreateResponse createPost(String email, PostCreateRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UUID authorId = user.getId();
        String safeContent = ContentSanitizer.minimal(request.content());
        Post post = Post.create(authorId, safeContent, request.visibility());

        List<String> urls = request.imageUrls() == null ? Collections.emptyList() : request.imageUrls();
        for (int i = 0; i < urls.size(); i++) {
            post.addImage(urls.get(i), i);
        }

        Post saved = postRepository.save(post);

        return new PostCreateResponse(
                saved.getId(),
                saved.getAuthorId(),
                saved.getContent(),
                saved.getImages().stream().map(img -> img.getImageUrl()).toList(),
                saved.getVisibility(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PostCreateResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        return new PostCreateResponse(
                post.getId(),
                post.getAuthorId(),
                post.getContent(),
                post.getImages().stream().map(img -> img.getImageUrl()).toList(),
                post.getVisibility(),
                post.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PostFeedResponse getFeed(String email, String cursor, int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);

        var me = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 나 + 내가 팔로우한 사람들
        var followingIds = followRepository.findAllByFollowerId(me.getId())
                .stream().map(f -> f.getFollowingId()).toList();

        java.util.Set<UUID> authorIds = new java.util.HashSet<>(followingIds);
        authorIds.add(me.getId());

        List<com.example.backend.domain.post.Post> posts;
        if (cursor == null || cursor.isBlank()) {
            posts = postRepository.findFollowingFeedFirstPage(authorIds, pageSize);
        } else {
            Cursor c = Cursor.decode(cursor);
            posts = postRepository.findFollowingFeedNextPage(authorIds, c.createdAt(), c.id(), pageSize);
        }

        var items = posts.stream().map(p ->
                new com.example.backend.controller.post.dto.PostFeedItem(
                        p.getId(),
                        p.getAuthorId(),
                        p.getContent(),
                        p.getImages().stream().map(img -> img.getImageUrl()).toList(),
                        p.getVisibility(),
                        p.getCreatedAt()
                )
        ).toList();

        boolean hasNext = items.size() == pageSize;
        String nextCursor = items.isEmpty()
                ? null
                : Cursor.encode(items.get(items.size() - 1).createdAt(), items.get(items.size() - 1).postId());

        if (!hasNext) nextCursor = null;

        return new com.example.backend.controller.post.dto.PostFeedResponse(items, nextCursor, hasNext);
    }

    private record Cursor(LocalDateTime createdAt, UUID id) {
        static String encode(LocalDateTime createdAt, UUID id) {
            return createdAt.toString() + "|" + id;
        }

        static Cursor decode(String cursor) {
            try {
                String[] parts = cursor.split("\\|");
                return new Cursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (Exception e) {
                throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다. (예: 2026-01-30T12:00:00|uuid)");
            }
        }
    }

    @Transactional
    public void deletePost(String email, UUID postId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        var post = postRepository.findByIdAndAuthorIdAndDeletedFalse(postId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("삭제 권한이 없거나 게시글이 존재하지 않습니다."));

        post.delete();
    }


}
