package com.example.backend.service.message;

import com.example.backend.domain.message.ConversationParticipant;
import com.example.backend.domain.message.Message;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageReadService {

    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void markAsRead(UUID meId, UUID conversationId) {

        ConversationParticipant participant =
                participantRepository.findByConversationIdAndUserId(conversationId, meId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        // ✅ 대화의 가장 최신 메시지 1개 조회
        Message lastMessage = messageRepository
                .findTopByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage == null) {
            return; // 메시지 없으면 할 거 없음
        }

        // 멱등 처리
        if (lastMessage.getId().equals(participant.getLastReadMessageId())) {
            return;
        }

        participant.markAsRead(lastMessage.getId());
    }
}