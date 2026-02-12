package com.example.backend.repository.message;

import com.example.backend.repository.message.dto.ConversationListRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ConversationListQueryRepository {

    List<ConversationListRow> fetchConversationList(
            UUID meId,
            LocalDateTime cursorAt,
            UUID cursorConversationId,
            int sizePlusOne
    );

    Map<UUID, Long> fetchUnreadCounts(UUID meId, List<UUID> conversationIds);
}