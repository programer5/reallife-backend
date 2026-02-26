package com.example.backend.domain.pin.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PinUpdatedEvent(
        UUID pinId,
        UUID conversationId,
        UUID actorId,
        String action,   // DONE|CANCELED|DISMISSED
        String status,   // ACTIVE|DONE|CANCELED
        LocalDateTime updatedAt
) {}