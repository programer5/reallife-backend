package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "conversation_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conversation_user",
                        columnNames = {"conversation_id", "user_id"}
                )
        }
)
public class ConversationParticipant extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ✅ 마지막 읽은 메시지
    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    private ConversationParticipant(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public static ConversationParticipant create(UUID conversationId, UUID userId) {
        return new ConversationParticipant(conversationId, userId);
    }

    public void markAsRead(UUID messageId) {
        this.lastReadMessageId = messageId;
    }
}