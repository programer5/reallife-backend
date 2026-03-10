package com.example.backend.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AdminHealthResponse(
        HealthStatus status,
        String service,
        String version,
        List<String> activeProfiles,
        Map<String, HealthStatus> checks,
        Map<String, Object> metrics,
        List<String> notes,
        LocalDateTime serverTime
) {
}