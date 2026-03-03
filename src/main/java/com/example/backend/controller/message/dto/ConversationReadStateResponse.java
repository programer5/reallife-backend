package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;

public record ConversationReadStateResponse(
        LocalDateTime lastReadAt
) {}