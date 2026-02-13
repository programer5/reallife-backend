package com.example.backend.service.message;

import com.example.backend.domain.message.event.MessageDeletedEvent;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.MessageHiddenRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.domain.message.MessageHidden;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageDeleteService {

    private final MessageRepository messageRepository;
    private final MessageHiddenRepository hiddenRepository;
    private final ConversationMemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 나만 삭제(숨김) */
    @Transactional
    public void hideForMe(UUID meId, UUID messageId) {
        var msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(msg.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        if (hiddenRepository.existsByUserIdAndMessageId(meId, messageId)) return;
        hiddenRepository.save(MessageHidden.hide(meId, messageId));
    }

    /** 모두 삭제(송신자만 가능) + SSE push */
    @Transactional
    public void deleteForAll(UUID meId, UUID messageId) {
        var msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(msg.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        if (!msg.getSenderId().equals(meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        msg.deleteForAll(now);

        // ✅ 커밋 성공 후에만 SSE 보내기 위해 이벤트 발행
        eventPublisher.publishEvent(new MessageDeletedEvent(
                msg.getId(),
                msg.getConversationId(),
                meId,
                now
        ));
    }
}