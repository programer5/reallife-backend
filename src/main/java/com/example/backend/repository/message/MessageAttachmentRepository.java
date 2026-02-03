package com.example.backend.repository.message;

import com.example.backend.domain.message.MessageAttachment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    @Query("select a from MessageAttachment a join fetch a.message where a.id = :id")
    Optional<MessageAttachment> findByIdWithMessage(@Param("id") UUID id);
}