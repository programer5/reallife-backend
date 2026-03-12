package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record GroupConversationCreateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        List<UUID> participantIds,

        UUID coverImageFileId
) {
}