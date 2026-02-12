package com.example.backend.repository.message;

import com.example.backend.domain.message.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("select m.userId from ConversationMember m where m.conversationId = :conversationId")
    List<UUID> findUserIdsByConversationId(UUID conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ConversationMember m
           set m.lastReadAt = :readAt
         where m.conversationId = :conversationId
           and m.userId = :userId
           and m.deleted = false
           and (m.lastReadAt is null or m.lastReadAt < :readAt)
    """)
    int updateLastReadAtIfLater(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("readAt") LocalDateTime readAt
    );

}