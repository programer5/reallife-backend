package com.example.backend.repository.message;

import com.example.backend.domain.message.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findAllByUserId(UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findAllByConversationId(UUID conversationId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("select p.userId from ConversationParticipant p where p.conversationId = :conversationId")
    List<UUID> findUserIdsByConversationId(UUID conversationId);

}