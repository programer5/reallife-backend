package com.example.backend.controller.search.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SearchReindexResponse(
        boolean elasticReady,
        String backend,
        String indexName,
        int batchSize,
        UUID requestedBy,
        LocalDateTime requestedAt,
        long durationMillis,
        Counts messages,
        Counts actions,
        Counts capsules,
        Counts posts,
        Totals totals
) {
    public record Counts(long indexed, long skipped) {}
    public record Totals(long indexed, long skipped) {}
}