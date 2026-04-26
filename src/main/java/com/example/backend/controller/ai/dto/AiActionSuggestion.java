package com.example.backend.controller.ai.dto;

import java.util.Map;

public record AiActionSuggestion(
        String type,
        String label,
        Map<String, Object> payload
) {}
