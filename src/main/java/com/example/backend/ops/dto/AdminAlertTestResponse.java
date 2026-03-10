package com.example.backend.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminAlertTestResponse {

    private boolean enabled;
    private boolean webhookConfigured;
    private boolean sent;

    private String channel;
    private String requestedBy;
    private String application;
    private String message;

    private LocalDateTime checkedAt;
}