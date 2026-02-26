package com.example.backend.domain.pin.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PinUpdatedEvent(
        UUID pinId,
        UUID conversationId,
        UUID actorId,

        String action,   // DONE | CANCELED | DISMISSED | UPDATED
        String status,   // ACTIVE | DONE | CANCELED

        String title,
        String placeText,
        LocalDateTime startAt,
        LocalDateTime remindAt,

        // ✅ targetUserId가 있으면 "해당 유저에게만" (dismiss 같은 per-user)
        UUID targetUserId,

        LocalDateTime updatedAt
) {}