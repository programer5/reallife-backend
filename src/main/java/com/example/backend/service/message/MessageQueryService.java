package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.message.MessageQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageQueryService {

    private final MessageQueryRepository messageQueryRepository;
    private final ConversationParticipantRepository participantRepository;

    public MessageListResponse list(
            UUID conversationId,
            UUID meId,
            LocalDateTime cursorCreatedAt,
            UUID cursorMessageId,
            int size
    ) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        int pageSize = Math.min(Math.max(size, 1), 50);

        List<MessageListResponse.Item> items =
                messageQueryRepository.fetchPage(conversationId, cursorCreatedAt, cursorMessageId, pageSize);

        boolean hasNext = items.size() > pageSize;
        if (hasNext) {
            items = items.subList(0, pageSize);
        }

        MessageListResponse.Cursor nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            var last = items.get(items.size() - 1);
            nextCursor = new MessageListResponse.Cursor(last.createdAt(), last.messageId());
        }

        return new MessageListResponse(items, nextCursor, hasNext);
    }
}