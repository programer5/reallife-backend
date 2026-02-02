package com.example.backend.service.like;

import com.example.backend.domain.like.PostLike;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.like.PostLikeRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void like(UUID meId, UUID postId) {

        // ✅ me 존재 확인(원하면 생략 가능)
        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // ✅ 멱등 처리: 이미 좋아요면 그냥 성공
        if (postLikeRepository.existsByPostIdAndUserId(postId, meId)) {
            return;
        }

        postLikeRepository.save(PostLike.create(postId, meId));
        post.increaseLikeCount();

        eventPublisher.publishEvent(new PostLikedEvent(postId, meId));
    }

    @Transactional
    public void unlike(UUID meId, UUID postId) {

        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        postLikeRepository.findByPostIdAndUserId(postId, meId)
                .ifPresent(like -> {
                    postLikeRepository.delete(like);
                    post.decreaseLikeCount();
                    eventPublisher.publishEvent(new PostUnlikedEvent(postId, meId));
                });
    }

    public record PostLikedEvent(UUID postId, UUID userId) {}
    public record PostUnlikedEvent(UUID postId, UUID userId) {}
}