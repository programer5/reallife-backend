package com.example.backend.controller.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreateResponse(
        UUID id,
        String email,
        String name,
        LocalDateTime createdAt
) {}
