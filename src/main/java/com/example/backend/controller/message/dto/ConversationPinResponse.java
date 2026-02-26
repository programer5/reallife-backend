package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationPinResponse(
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