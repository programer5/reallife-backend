package com.example.backend.service.message;

import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.message.Message;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageReadService {

    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void markAsRead(UUID meId, UUID conversationId) {
        ConversationMember member =
                memberRepository.findByConversationIdAndUserId(conversationId, meId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        Message lastMessage = messageRepository
                .findTopByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversationId)
                .orElse(null);

        if (lastMessage == null) return;
        if (lastMessage.getId().equals(member.getLastReadMessageId())) return;

        member.markRead(lastMessage.getId());
    }
}