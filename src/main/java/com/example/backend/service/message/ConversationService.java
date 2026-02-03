package com.example.backend.service.message;

import com.example.backend.controller.message.dto.ConversationResponse;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse createOrGetDirect(UUID meId, UUID targetUserId) {

        if (meId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST); // 원하면 별도 코드 추가
        }

        // ✅ 존재 검증 (깔끔한 에러를 위해)
        userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.findById(targetUserId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ✅ 이미 DIRECT 대화방 있으면 재사용
        var existing = conversationRepository.findExistingDirectConversation(meId, targetUserId);
        if (existing.isPresent()) {
            UUID convId = existing.get();
            return new ConversationResponse(
                    convId,
                    "DIRECT",
                    List.of(new ConversationResponse.Member(meId), new ConversationResponse.Member(targetUserId))
            );
        }

        // ✅ 없으면 생성
        Conversation conv = conversationRepository.save(Conversation.direct());

        memberRepository.save(ConversationMember.join(conv.getId(), meId));
        memberRepository.save(ConversationMember.join(conv.getId(), targetUserId));

        return new ConversationResponse(
                conv.getId(),
                "DIRECT",
                List.of(new ConversationResponse.Member(meId), new ConversationResponse.Member(targetUserId))
        );
    }
}