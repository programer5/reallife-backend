package com.example.backend.controller.pin.dto;

import java.time.LocalDateTime;

public record ConversationPinUpdateRequest(
        String title,
        String placeText,
        LocalDateTime startAt,
        Integer remindMinutes // ✅ NEW
) {}