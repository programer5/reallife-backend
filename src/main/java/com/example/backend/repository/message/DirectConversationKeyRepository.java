package com.example.backend.repository.message;

import com.example.backend.domain.message.DirectConversationKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DirectConversationKeyRepository extends JpaRepository<DirectConversationKey, UUID> {

    Optional<DirectConversationKey> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);
}