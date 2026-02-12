package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationListResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            UUID conversationId,
            PeerUser peerUser,
            String lastMessagePreview,
            LocalDateTime lastMessageAt,
            long unreadCount
    ) {}

    public record PeerUser(
            UUID userId,
            String nickname,
            String profileImageUrl
    ) {}
}