package com.example.backend.controller.message.dto;

import java.util.UUID;

public record DirectConversationCreateResponse(
        UUID conversationId
) {}