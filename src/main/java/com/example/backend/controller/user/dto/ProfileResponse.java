package com.example.backend.controller.user.dto;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String handle,
        String name,
        String bio,
        String website,
        String profileImageUrl,
        long followerCount,
        long followingCount
) {}