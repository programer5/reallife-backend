package com.example.backend.service.follow;

import com.example.backend.domain.follow.Follow;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.follow.FollowRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void follow(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (followRepository.existsByFollowerIdAndFollowingIdAndDeletedFalse(meId, targetUserId)) {
            return;
        }

        Follow follow = followRepository.findByFollowerIdAndFollowingId(meId, targetUserId)
                .map(f -> {
                    f.restore();
                    return f;
                })
                .orElseGet(() -> Follow.create(meId, targetUserId));

        followRepository.save(follow);

        // ✅ 커밋 이후 알림 생성 + SSE push를 위해 이벤트 발행
        eventPublisher.publishEvent(new UserFollowedEvent(meId, targetUserId));
    }

    @Transactional
    public void unfollow(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        followRepository.findByFollowerIdAndFollowingId(meId, targetUserId)
                .ifPresent(f -> {
                    f.softDelete();
                });
    }

    public record UserFollowedEvent(UUID followerId, UUID targetUserId) {}
}
