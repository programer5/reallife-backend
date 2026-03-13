
package com.example.backend.repository.message;

import com.example.backend.domain.message.MessageCapsule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageCapsuleRepository extends JpaRepository<MessageCapsule, UUID> {
    List<MessageCapsule> findByConversationIdOrderByUnlockAtDesc(UUID conversationId);
}
