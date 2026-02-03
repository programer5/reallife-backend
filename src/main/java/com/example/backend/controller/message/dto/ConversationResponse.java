package com.example.backend.controller.message.dto;

import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        String type,
        List<Member> members
) {
    public record Member(UUID userId) {}
}