package com.example.backend.controller.search.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SearchResponse(
        String query,
        String type,
        UUID conversationId,
        List<Section> sections,
        List<Item> items,
        Meta meta
) {
    public record Section(
            String type,
            String label,
            long count
    ) {}

    public record Item(
            String type,
            UUID id,
            String title,
            String snippet,
            String highlight,
            LocalDateTime createdAt,
            UUID conversationId,
            String deepLink,
            String badge,
            String secondary,
            String anchorType,
            UUID anchorId,
            Integer relevance
    ) {}

    public record Meta(
            boolean elasticReady,
            String backend,
            int requestedLimit,
            List<String> availableTypes
    ) {}
}
