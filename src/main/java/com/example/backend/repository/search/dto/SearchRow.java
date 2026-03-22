package com.example.backend.repository.search.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SearchRow(
        String type,
        UUID id,
        String title,
        String snippet,
        String highlight,
        LocalDateTime createdAt,
        UUID conversationId,
        String badge,
        String secondary,
        Integer relevanceScore
) {}
