package com.example.backend.service.follow;

import com.example.backend.domain.follow.Follow;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void follow(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 존재 검증 (원하면 생략 가능하지만 API 안정성을 위해 추천)
        userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 팔로우면 그냥 멱등 처리(204 유지) 또는 에러 처리 선택 가능
        if (followRepository.existsByFollowerIdAndFollowingId(meId, targetUserId)) {
            return;
        }

        followRepository.save(Follow.create(meId, targetUserId));
        targetUser.increaseFollowerCount();
    }

    @Transactional
    public void unfollow(UUID meId, UUID targetUserId) {
        followRepository.findByFollowerIdAndFollowingId(meId, targetUserId)
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    userRepository.findById(targetUserId)
                            .ifPresent(user -> user.decreaseFollowerCount());
                });
    }
}