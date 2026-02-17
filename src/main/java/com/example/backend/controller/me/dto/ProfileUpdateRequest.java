package com.example.backend.controller.me.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProfileUpdateRequest(
        @Size(max = 255) String bio,
        @Size(max = 255) String website,
        UUID profileImageFileId
) {}