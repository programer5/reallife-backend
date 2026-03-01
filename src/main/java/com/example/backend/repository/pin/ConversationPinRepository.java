package com.example.backend.repository.pin;

import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.pin.PinStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // ✅ 같은 메시지 기반 핀 중복 생성 방지
    boolean existsByConversationIdAndSourceMessageIdAndDeletedFalse(UUID conversationId, UUID sourceMessageId);

    Optional<ConversationPin> findByConversationIdAndSourceMessageIdAndDeletedFalse(
            UUID conversationId,
            UUID sourceMessageId
    );

    // ✅ NEW: remindAt이 지난 ACTIVE 핀들(배치용)
    @Query("""
        select p
          from ConversationPin p
         where p.deleted = false
           and p.status = com.example.backend.domain.pin.PinStatus.ACTIVE
           and p.remindAt is not null
           and p.remindAt <= :now
           and p.remindedAt is null
         order by p.remindAt asc
    """)
    List<ConversationPin> findDueReminds(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
    select p
      from ConversationPin p
     where p.deleted = false
       and p.conversationId = :conversationId
       and p.status = com.example.backend.domain.pin.PinStatus.ACTIVE
       and not exists (
            select 1
              from ConversationPinDismissal d
             where d.pinId = p.id
               and d.userId = :userId
       )
     order by p.createdAt desc
    """)
    List<ConversationPin> findActivePinsVisibleToUser(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
    update ConversationPin p
       set p.remindedAt = :now
     where p.id = :pinId
       and p.remindedAt is null
""")
    int claimRemind(@Param("pinId") UUID pinId, @Param("now") LocalDateTime now);
}