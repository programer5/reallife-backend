package com.example.backend.service.message;

import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationParticipant;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID createOrGetDirect(UUID meId, UUID targetUserId) {
        if (meId.equals(targetUserId)) {
            // FOLLOW_CANNOT_FOLLOW_SELF 같은 별도 코드가 있으면 그걸 써도 됨
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        userRepository.findById(meId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.findById(targetUserId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ✅ DIRECT 대화방 “중복 방지”를 완벽히 하려면 (meId,targetId) 조합을 저장하는 별도 테이블이 제일 깔끔.
        // 이번 단계는 간단히: "이미 둘 다 참가한 대화방"이 있는지 조회해서 있으면 재사용.
        // (규모 커지면 ConversationDirectKey 테이블로 개선 추천)

        var myRooms = participantRepository.findAllByUserId(meId);
        for (var p : myRooms) {
            UUID roomId = p.getConversationId();
            boolean targetIn = participantRepository.existsByConversationIdAndUserId(roomId, targetUserId);
            if (targetIn) {
                return roomId;
            }
        }

        Conversation c = conversationRepository.save(Conversation.direct());
        participantRepository.save(ConversationParticipant.create(c.getId(), meId));
        participantRepository.save(ConversationParticipant.create(c.getId(), targetUserId));
        return c.getId();
    }
}