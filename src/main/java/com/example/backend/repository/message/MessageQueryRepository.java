package com.example.backend.repository.message;

import com.example.backend.controller.message.dto.MessageListResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageQueryRepository {

    List<MessageListResponse.Item> fetchPage(UUID conversationId, LocalDateTime cursorCreatedAt, UUID cursorMessageId, int size);

    List<MessageListResponse.Item> fetchPage(UUID conversationId, UUID meId, LocalDateTime cursorCreatedAt, UUID cursorMessageId, int limit);
}