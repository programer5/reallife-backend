package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationUnlockTokenRequest(
        @NotBlank
        String password
) {}