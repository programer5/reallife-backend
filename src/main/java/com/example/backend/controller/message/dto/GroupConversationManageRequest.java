
package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GroupConversationManageRequest(
        @NotBlank
        @Size(max = 255)
        String title,
        UUID coverImageFileId
) {}
