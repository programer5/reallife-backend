package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "conversation_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conv_user", columnNames = {"conversation_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_conv_id", columnList = "conversation_id"),
                @Index(name = "idx_user_id", columnList = "user_id")
        }
)
public class ConversationMember extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    private ConversationMember(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public static ConversationMember join(UUID conversationId, UUID userId) {
        return new ConversationMember(conversationId, userId);
    }

    public void markRead(UUID messageId) {
        this.lastReadMessageId = messageId;
    }
}