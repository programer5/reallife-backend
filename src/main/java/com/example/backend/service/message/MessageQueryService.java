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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageQueryService {

    private final MessageQueryRepository messageQueryRepository;
    private final ConversationParticipantRepository participantRepository; // ✅ 통일

    public MessageListResponse list(
            UUID conversationId,
            UUID meId,
            String cursor,
            int size
    ) {
        // ✅ 권한 체크: participants 기준으로 통일
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        int pageSize = Math.min(Math.max(size, 1), 50);

        Cursor decoded = Cursor.decode(cursor);

        var items = messageQueryRepository.fetchPage(
                conversationId,
                decoded.createdAt(),
                decoded.messageId(),
                pageSize + 1
        );

        boolean hasNext = items.size() > pageSize;
        if (hasNext) items = items.subList(0, pageSize);

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            var last = items.get(items.size() - 1);
            nextCursor = Cursor.encode(last.createdAt(), last.messageId());
        }

        return new MessageListResponse(items, nextCursor, hasNext);
    }

    // cursor 형식: "2026-02-06T11:41:49.123|uuid"
    private record Cursor(LocalDateTime createdAt, UUID messageId) {
        static Cursor decode(String raw) {
            if (raw == null || raw.isBlank()) return new Cursor(null, null);

            try {
                String[] parts = raw.split("\\|");
                if (parts.length != 2) return new Cursor(null, null);

                return new Cursor(
                        LocalDateTime.parse(parts[0]),
                        UUID.fromString(parts[1])
                );
            } catch (Exception e) {
                // 커서가 이상하면 첫 페이지로 처리(멱등)
                return new Cursor(null, null);
            }
        }

        static String encode(LocalDateTime createdAt, UUID messageId) {
            return createdAt + "|" + messageId;
        }
    }
}