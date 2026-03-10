package com.example.backend.controller.admin.dto;

import java.time.LocalDateTime;

public record AdminAlertTestResponse(
        boolean enabled,
        boolean webhookConfigured,
        boolean sent,
        String channel,
        String requestedBy,
        String application,
        String message,
        LocalDateTime checkedAt
) {
}