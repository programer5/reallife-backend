package com.example.backend.controller.ai.dto;

import java.util.Map;

public record AiActionExecuteResponse(
        String status,
        String type,
        String message,
        String targetUrl,
        Map<String, Object> payload
) {}
