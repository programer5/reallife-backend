package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConversationReadReceiptsResponse(
        List<Item> items
) {
    public record Item(
            UUID userId,
            LocalDateTime lastReadAt
    ) {}
}