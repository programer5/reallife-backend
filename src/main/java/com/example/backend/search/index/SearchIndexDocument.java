package com.example.backend.search.index;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SearchIndexDocument(
        String type,
        UUID id,
        UUID conversationId,
        String title,
        String body,
        String badge,
        String secondary,
        String anchorType,
        UUID anchorId,
        String deepLink,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tags
) {}
