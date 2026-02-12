package com.example.backend.repository.message;

import com.example.backend.domain.message.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationMember> findAllByConversationId(UUID conversationId);

    // ✅ 핵심: 두 유저가 동시에 들어있는 conversationId 후보
    List<ConversationMember> findAllByUserId(UUID userId);

    @Query("select m.userId from ConversationMember m where m.conversationId = :conversationId")
    List<UUID> findUserIdsByConversationId(UUID conversationId);

}