package com.example.backend.controller.user.dto;

public record MeResponse(
        String id,
        String email,
        String handle,
        String name,
        long followerCount,
        String tier
) {}
