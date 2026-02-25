package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationLockSetRequest(
        @NotBlank
        @Size(min = 4, max = 64)
        String password
) {}