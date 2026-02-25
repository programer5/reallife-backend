package com.example.backend.service.message;

import com.example.backend.domain.message.ConversationMember;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationLockService {

    // 토큰 TTL (운영에서 조절 가능)
    private static final Duration UNLOCK_TOKEN_TTL = Duration.ofMinutes(30);

    private final ConversationMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    @Transactional(readOnly = true)
    public boolean isLockEnabled(UUID conversationId, UUID userId) {
        ConversationMember m = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));
        return m.isLockEnabled();
    }

    @Transactional
    public void enableLock(UUID conversationId, UUID userId, String rawPassword) {
        ConversationMember m = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        String hash = passwordEncoder.encode(rawPassword);
        m.enableLock(hash);
    }

    @Transactional
    public void disableLock(UUID conversationId, UUID userId, String rawPassword) {
        ConversationMember m = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        if (!m.isLockEnabled()) return;

        if (m.getLockPasswordHash() == null || !passwordEncoder.matches(rawPassword, m.getLockPasswordHash())) {
            throw new BusinessException(ErrorCode.CONVERSATION_LOCK_PASSWORD_INVALID);
        }

        m.disableLock();
    }

    @Transactional(readOnly = true)
    public ConversationUnlock issueUnlockToken(UUID conversationId, UUID userId, String rawPassword) {
        ConversationMember m = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        if (!m.isLockEnabled()) {
            // 잠금이 없으면 토큰 필요 없음
            return new ConversationUnlock("", Instant.now());
        }

        if (m.getLockPasswordHash() == null || !passwordEncoder.matches(rawPassword, m.getLockPasswordHash())) {
            throw new BusinessException(ErrorCode.CONVERSATION_LOCK_PASSWORD_INVALID);
        }

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(UNLOCK_TOKEN_TTL);

        String key = unlockKey(userId, conversationId, m.getLockVersion(), token);
        redis.opsForValue().set(key, "1", UNLOCK_TOKEN_TTL);

        return new ConversationUnlock(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public void ensureUnlocked(UUID conversationId, UUID userId, String unlockToken) {
        ConversationMember m = memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        if (!m.isLockEnabled()) return;

        if (unlockToken == null || unlockToken.isBlank()) {
            throw new BusinessException(ErrorCode.CONVERSATION_LOCKED);
        }

        String key = unlockKey(userId, conversationId, m.getLockVersion(), unlockToken);
        Boolean ok = redis.hasKey(key);

        if (ok == null || !ok) {
            throw new BusinessException(ErrorCode.CONVERSATION_LOCKED);
        }
    }

    private String unlockKey(UUID userId, UUID conversationId, String lockVersion, String token) {
        String v = (lockVersion == null || lockVersion.isBlank()) ? "v0" : lockVersion;
        return "conv_unlock:" + userId + ":" + conversationId + ":" + v + ":" + token;
    }

    public record ConversationUnlock(String token, Instant expiresAt) {}
}