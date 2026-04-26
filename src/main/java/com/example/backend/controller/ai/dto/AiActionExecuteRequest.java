package com.example.backend.controller.ai.dto;

import java.util.Map;
import java.util.UUID;

public record AiActionExecuteRequest(
        UUID conversationId,
        UUID messageId,
        String type,
        String text,
        Map<String, Object> payload
) {}
