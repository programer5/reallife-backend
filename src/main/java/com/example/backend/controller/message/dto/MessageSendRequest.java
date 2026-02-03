package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record MessageSendRequest(
        @Size(max = 5000) String content,
        List<UUID> attachmentIds
) {}