package com.example.backend.repository.user.dto;

import java.util.UUID;

public record UserSearchRow(
        UUID userId,
        String handle,
        String name,
        long followerCount,
        int rank
) {}