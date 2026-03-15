package com.example.backend.controller.message.dto;

public record MessageCapsuleUpdateRequest(
        String title,
        String unlockAt
) {}
