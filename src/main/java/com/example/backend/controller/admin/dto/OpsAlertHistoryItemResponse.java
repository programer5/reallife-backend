package com.example.backend.controller.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpsAlertHistoryItemResponse(
        UUID id,
        String channel,
        String alertKey,
        String title,
        String body,
        String level,
        String status,
        String requestedBy,
        LocalDateTime createdAt
) {
}