package com.example.backend.controller.home.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HomeTodayWidgetResponse(
        Summary summary,
        List<Item> items
) {
    public record Summary(
            int total,
            int upcoming,
            int done
    ) {}

    public record Item(
            UUID pinId,
            UUID conversationId,
            UUID sourceMessageId,
            String type,
            String title,
            String placeText,
            LocalDateTime startAt,
            LocalDateTime remindAt,
            String status
    ) {}
}
