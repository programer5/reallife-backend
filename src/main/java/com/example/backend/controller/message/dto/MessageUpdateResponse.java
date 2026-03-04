// MessageUpdateResponse.java
package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageUpdateResponse(
        UUID messageId,
        UUID conversationId,
        String content,
        LocalDateTime editedAt
) {}