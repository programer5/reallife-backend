package com.example.backend.repository.pin;

import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.pin.PinStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationPinRepository extends JpaRepository<ConversationPin, UUID> {

    List<ConversationPin> findByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID conversationId,
            PinStatus status,
            Pageable pageable
    );

    Optional<ConversationPin> findTopByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID conversationId,
            PinStatus status
    );
}