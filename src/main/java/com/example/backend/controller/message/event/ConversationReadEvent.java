package com.example.backend.domain.message.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationReadEvent(
        UUID conversationId,
        UUID userId,
        LocalDateTime lastReadAt
) {}