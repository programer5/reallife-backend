package com.example.backend.service.follow;

import com.example.backend.domain.follow.Follow;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF); // 너 enum에 맞춰 사용
            // 네 ErrorCode 이름이 FOLLOW_CANNOT_FOLLOW_SELF면 그걸로 바꿔
        }

        // target 존재 검증 (원하면 생략 가능)
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 active면 멱등 성공
        if (followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(meId, targetUserId)) {
            return;
        }

        // 기존 row가 있으면 restore, 없으면 생성
        Follow follow = followRepository.findByFollowerIdAndFollowingId(meId, targetUserId)
                .map(f -> {
                    f.restore();
                    return f;
                })
                .orElseGet(() -> Follow.create(meId, targetUserId));

        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        followRepository.findByFollowerIdAndFollowingId(meId, targetUserId)
                .ifPresent(f -> {
                    // 멱등: 이미 deleted여도 그냥 성공
                    f.softDelete();
                });
    }
}
