package com.example.backend.repository.message;

import com.example.backend.domain.message.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
    Optional<MessageAttachment> findById(UUID id);
    List<MessageAttachment> findAllByMessageId(UUID messageId);
    Optional<MessageAttachment> findTopByFileIdOrderByCreatedAtDesc(UUID fileId);

}