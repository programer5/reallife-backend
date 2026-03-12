package com.example.backend.repository.message.dto;

import com.example.backend.domain.message.ConversationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationListRow(
        UUID conversationId,
        ConversationType conversationType,
        String conversationTitle,
        UUID peerUserId,
        String peerNickname,
        String peerProfileImageUrl,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        LocalDateTime sortAt
) {
}