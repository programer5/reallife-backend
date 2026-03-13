
package com.example.backend.domain.message;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "message_capsules")
public class MessageCapsule {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "unlock_at", nullable = false)
    private LocalDateTime unlockAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    private MessageCapsule(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.creatorId = creatorId;
        this.title = title;
        this.unlockAt = unlockAt;
    }

    public static MessageCapsule create(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt) {
        return new MessageCapsule(messageId, conversationId, creatorId, title, unlockAt);
    }

    public void open() {
        this.openedAt = LocalDateTime.now();
    }
}
