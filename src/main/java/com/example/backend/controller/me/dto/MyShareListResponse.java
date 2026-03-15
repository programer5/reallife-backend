package com.example.backend.controller.me.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MyShareListResponse(
        List<Item> items
) {
    public record Item(
            UUID postId,
            String content,
            String visibility,
            LocalDateTime createdAt
    ) {}
}
