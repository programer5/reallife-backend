package com.example.backend.repository.message;

import java.util.Optional;
import java.util.UUID;

public interface ConversationQueryRepository {
    Optional<UUID> findExistingDirectConversation(UUID meId, UUID targetUserId);
}