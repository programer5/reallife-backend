package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationLockDisableRequest(
        @NotBlank
        String password
) {}