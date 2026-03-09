package com.example.backend.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RealtimeHealthResponse {

    private HealthStatus status;

    private int activeSseConnections;
    private LocalDateTime lastSseEventSentAt;
    private LocalDateTime lastNotificationCreatedAt;
    private LocalDateTime lastMessageNotificationCreatedAt;
    private LocalDateTime lastPinRemindNotificationCreatedAt;

    private List<String> notes;
    private LocalDateTime serverTime;
}