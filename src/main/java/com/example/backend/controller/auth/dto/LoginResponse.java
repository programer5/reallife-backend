package com.example.backend.controller.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {}