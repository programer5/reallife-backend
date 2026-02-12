package com.example.backend.service.message;

import com.example.backend.controller.message.dto.ConversationListResponse;
import com.example.backend.repository.message.ConversationListQueryRepository;
import com.example.backend.repository.message.dto.ConversationListRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationListService {

    private final ConversationListQueryRepository queryRepository;

    public ConversationListResponse list(UUID meId, String cursor, int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);
        int sizePlusOne = pageSize + 1;

        CursorDecoded decoded = CursorDecoded.decode(cursor);

        List<ConversationListRow> rows = queryRepository.fetchConversationList(
                meId, decoded.cursorAt(), decoded.cursorConversationId(), sizePlusOne
        );

        boolean hasNext = rows.size() > pageSize;
        List<ConversationListRow> page = hasNext ? rows.subList(0, pageSize) : rows;

        List<UUID> convIds = page.stream().map(ConversationListRow::conversationId).toList();
        Map<UUID, Long> unreadMap = queryRepository.fetchUnreadCounts(meId, convIds);

        List<ConversationListResponse.Item> items = page.stream()
                .map(r -> new ConversationListResponse.Item(
                        r.conversationId(),
                        new ConversationListResponse.PeerUser(
                                r.peerUserId(),
                                r.peerNickname(),
                                r.peerProfileImageUrl()
                        ),
                        r.lastMessagePreview(),
                        r.lastMessageAt(),
                        unreadMap.getOrDefault(r.conversationId(), 0L)
                ))
                .toList();

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            ConversationListRow last = page.get(page.size() - 1);
            nextCursor = CursorDecoded.encode(last.sortAt(), last.conversationId());
        }

        return new ConversationListResponse(items, nextCursor, hasNext);
    }

    private record CursorDecoded(LocalDateTime cursorAt, UUID cursorConversationId) {

        static CursorDecoded decode(String cursor) {
            if (cursor == null || cursor.isBlank()) return new CursorDecoded(null, null);
            String[] parts = cursor.split("_", 2);
            long millis = Long.parseLong(parts[0]);
            UUID id = UUID.fromString(parts[1]);

            // 서버 타임존을 고정하고 싶으면 Asia/Seoul로 변경 가능
            LocalDateTime at = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime();
            return new CursorDecoded(at, id);
        }

        static String encode(LocalDateTime at, UUID id) {
            long millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return millis + "_" + id;
        }
    }
}