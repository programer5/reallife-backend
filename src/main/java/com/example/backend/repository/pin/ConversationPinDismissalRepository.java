package com.example.backend.repository.pin;

import com.example.backend.domain.pin.ConversationPinDismissal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationPinDismissalRepository extends JpaRepository<ConversationPinDismissal, UUID> {

    boolean existsByPinIdAndUserId(UUID pinId, UUID userId);

    Optional<ConversationPinDismissal> findByPinIdAndUserId(UUID pinId, UUID userId);

    List<ConversationPinDismissal> findAllByUserIdAndPinIdIn(UUID userId, List<UUID> pinIds);
}