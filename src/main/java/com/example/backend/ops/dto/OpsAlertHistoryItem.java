package com.example.backend.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class OpsAlertHistoryItem {

    private Long id;
    private String channel;
    private String alertKey;
    private String title;
    private String body;
    private String level;
    private String status;
    private String requestedBy;
    private LocalDateTime createdAt;
}