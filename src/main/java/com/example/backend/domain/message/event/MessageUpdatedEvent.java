package com.example.backend.domain.message.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageUpdatedEvent(
        UUID messageId,
        UUID conversationId,
        UUID editorId,
        String content,
        LocalDateTime editedAt
) {}