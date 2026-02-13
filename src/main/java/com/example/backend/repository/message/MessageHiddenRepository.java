package com.example.backend.repository.message;

import com.example.backend.domain.message.MessageHidden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageHiddenRepository extends JpaRepository<MessageHidden, UUID> {
    boolean existsByUserIdAndMessageId(UUID userId, UUID messageId);
    void deleteByUserIdAndMessageId(UUID userId, UUID messageId);
}