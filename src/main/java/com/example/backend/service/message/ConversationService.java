package com.example.backend.service.message;

import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.DirectConversationKey;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.DirectConversationKeyRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final DirectConversationKeyRepository directKeyRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createOrGetDirect(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.findById(targetUserId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID u1 = (meId.compareTo(targetUserId) <= 0) ? meId : targetUserId;
        UUID u2 = (meId.compareTo(targetUserId) <= 0) ? targetUserId : meId;

        // 1) 먼저 key 테이블에서 조회
        var existing = directKeyRepository.findByUser1IdAndUser2Id(u1, u2);
        if (existing.isPresent()) {
            return existing.get().getConversationId();
        }

        // 2) 없으면 생성 (동시성은 UNIQUE로 해결)
        Conversation c = conversationRepository.save(Conversation.direct());

        memberRepository.save(ConversationMember.join(c.getId(), meId));
        memberRepository.save(ConversationMember.join(c.getId(), targetUserId));

        try {
            directKeyRepository.save(DirectConversationKey.of(c.getId(), meId, targetUserId));
            return c.getId();
        } catch (DataIntegrityViolationException e) {
            // 누가 먼저 만들었다 -> 재조회
            return directKeyRepository.findByUser1IdAndUser2Id(u1, u2)
                    .map(DirectConversationKey::getConversationId)
                    .orElseThrow(() -> e);
        }
    }
}