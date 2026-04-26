package com.example.backend.controller.ai.dto;

import java.util.List;

public record AiReplyResponse(
        List<String> replies,
        List<AiActionSuggestion> actions,
        String source
) {}
