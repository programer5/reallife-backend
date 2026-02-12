package com.example.backend.repository.message.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationListRow(
        UUID conversationId,
        UUID peerUserId,
        String peerNickname,
        String peerProfileImageUrl,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime sortAt
) {}