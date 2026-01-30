package com.example.backend.service.follow;

import com.example.backend.domain.follow.Follow;
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
    public void follow(String email, UUID targetUserId) {
        var me = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (me.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        // target 존재 확인
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 대상 사용자를 찾을 수 없습니다."));

        if (followRepository.existsByFollowerIdAndFollowingId(me.getId(), targetUserId)) {
            return; // 멱등 처리 (이미 팔로우면 그냥 성공)
        }

        followRepository.save(Follow.create(me.getId(), targetUserId));
    }

    @Transactional
    public void unfollow(String email, UUID targetUserId) {
        var me = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        followRepository.findByFollowerIdAndFollowingId(me.getId(), targetUserId)
                .ifPresent(followRepository::delete); // 멱등 처리
    }
}
