package com.example.backend.controller.ai.dto;

import java.util.UUID;

public record AiReplyRequest(
        UUID conversationId,
        UUID messageId,
        String text
) {}
