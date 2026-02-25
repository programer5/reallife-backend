package com.example.backend.controller.message.dto;

import java.time.Instant;

public record ConversationUnlockTokenResponse(
        String token,
        Instant expiresAt
) {}