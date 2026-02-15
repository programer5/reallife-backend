package com.example.backend.service.auth.dto;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {}