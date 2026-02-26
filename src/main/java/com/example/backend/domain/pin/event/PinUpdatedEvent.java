package com.example.backend.domain.pin.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PinUpdatedEvent(
        UUID pinId,
        UUID conversationId,
        UUID actorId,

        String action,   // DONE|CANCELED|DISMISSED
        String status,   // ACTIVE|DONE|CANCELED

        // ✅ NEW: 프론트 UX 위해 payload 보강
        String title,
        String placeText,
        LocalDateTime startAt,
        LocalDateTime remindAt,

        LocalDateTime updatedAt
) {}