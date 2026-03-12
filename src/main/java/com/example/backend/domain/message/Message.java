package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
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
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at")
        }
)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    private Message(
            UUID conversationId,
            UUID senderId,
            MessageType type,
            String content,
            String metadataJson,
            LocalDateTime lockedUntil,
            UUID sessionId
    ) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.metadataJson = metadataJson;
        this.lockedUntil = lockedUntil;
        this.sessionId = sessionId;
    }

    public static Message text(UUID conversationId, UUID senderId, String content) {
        return new Message(conversationId, senderId, MessageType.TEXT, content, null, null, null);
    }

    public static Message file(UUID conversationId, UUID senderId, String content, String metadataJson) {
        return new Message(conversationId, senderId, MessageType.FILE, content, metadataJson, null, null);
    }

    public static Message capsule(
            UUID conversationId,
            UUID senderId,
            String content,
            String metadataJson,
            LocalDateTime lockedUntil
    ) {
        return new Message(conversationId, senderId, MessageType.CAPSULE, content, metadataJson, lockedUntil, null);
    }

    public static Message session(
            UUID conversationId,
            UUID senderId,
            String content,
            String metadataJson,
            UUID sessionId
    ) {
        return new Message(conversationId, senderId, MessageType.SESSION, content, metadataJson, null, sessionId);
    }

    public static Message system(UUID conversationId, UUID senderId, String content, String metadataJson) {
        return new Message(conversationId, senderId, MessageType.SYSTEM, content, metadataJson, null, null);
    }

    public void deleteForAll(LocalDateTime now) {
        this.markDeleted();
        this.deletedAt = now;
    }

    public void updateContent(String newContent, LocalDateTime now) {
        this.content = newContent;
        this.editedAt = now;
    }
}