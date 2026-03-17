package com.example.backend.controller.home.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HomeReminderSummaryResponse(
        Summary summary,
        Settings settings,
        Lead lead
) {
    public record Summary(
            long unreadCount,
            long unreadReminderCount,
            long todayReminderCount
    ) {}

    public record Settings(
            boolean browserNotifyEnabled,
            String settingsSource
    ) {}

    public record Lead(
            UUID id,
            String type,
            UUID refId,
            UUID ref2Id,
            UUID conversationId,
            String body,
            boolean read,
            LocalDateTime createdAt
    ) {}
}

