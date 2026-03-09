package com.example.backend.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class AdminHealthResponse {

    private HealthStatus status;
    private String service;
    private String version;
    private List<String> activeProfiles;

    private Map<String, HealthStatus> checks;
    private Map<String, Object> metrics;

    private List<String> notes;
    private LocalDateTime serverTime;
}