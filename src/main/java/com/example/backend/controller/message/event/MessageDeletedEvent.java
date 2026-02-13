package com.example.backend.domain.message.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageDeletedEvent(
        UUID messageId,
        UUID conversationId,
        UUID deletedByUserId,
        LocalDateTime deletedAt
) {
}