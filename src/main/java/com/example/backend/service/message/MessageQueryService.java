package com.example.backend.service.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.domain.message.Conversation;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
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
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    /**
     * GET /api/conversations/{conversationId}/messages
     * - 404: conversation 없음
     * - 403: 멤버 아님
     * - 목록 조회 성공 시: 자동 읽음 처리(last_read_at 갱신)
     */
    @Transactional // ✅ readOnly=false (자동 읽음 처리 update 때문에)
    public MessageListResponse list(
            UUID conversationId,
            UUID meId,
            String cursor,
            int size
    ) {
        // ✅ 1) 존재 확인 먼저 (404)
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        // ✅ 2) 권한 확인 (403)
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        int pageSize = Math.min(Math.max(size, 1), 50);
        Cursor decoded = Cursor.decode(cursor);

        var items = messageQueryRepository.fetchPage(
                conversationId,
                meId,                 // ✅ 추가
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

        // ✅ 3) 자동 읽음 처리: 대화방 최신 메시지 시각까지 last_read_at 갱신
        LocalDateTime lastAt = conversation.getLastMessageAt();
        if (lastAt != null) {
            memberRepository.updateLastReadAtIfLater(conversationId, meId, lastAt);
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