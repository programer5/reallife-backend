package com.example.backend.repository.message;

import com.example.backend.domain.message.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
        select m from Message m
        where m.conversationId = :conversationId
          and m.deleted = false
        order by m.createdAt desc, m.id desc
    """)
    List<Message> findFirstPage(UUID conversationId, Pageable pageable);

    @Query("""
        select m from Message m
        where m.conversationId = :conversationId
          and m.deleted = false
          and (
              m.createdAt < :cursorCreatedAt
              or (m.createdAt = :cursorCreatedAt and m.id < :cursorId)
          )
        order by m.createdAt desc, m.id desc
    """)
    List<Message> findNextPage(UUID conversationId, LocalDateTime cursorCreatedAt, UUID cursorId, Pageable pageable);
    Optional<Message> findByIdAndDeletedFalse(UUID id);

    Optional<Message>
    findTopByConversationIdAndDeletedFalseOrderByCreatedAtDesc(UUID conversationId);

}