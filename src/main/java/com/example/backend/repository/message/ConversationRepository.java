package com.example.backend.repository.message;

import com.example.backend.domain.message.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>, ConversationQueryRepository {
}