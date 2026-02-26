package com.example.backend.domain.pin.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PinCreatedEvent(
        UUID pinId,
        UUID conversationId,
        UUID createdBy,
        String type,
        String title,
        String placeText,
        LocalDateTime startAt,
        LocalDateTime remindAt,
        String status,
        LocalDateTime createdAt
) {}