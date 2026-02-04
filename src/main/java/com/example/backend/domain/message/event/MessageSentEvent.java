package com.example.backend.domain.message.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageSentEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String content,
        LocalDateTime createdAt
) {
}