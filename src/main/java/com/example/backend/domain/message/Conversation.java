package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "cover_image_file_id")
    private UUID coverImageFileId;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    private Conversation(ConversationType type, String title, UUID ownerId, UUID coverImageFileId) {
        this.type = type;
        this.title = title;
        this.ownerId = ownerId;
        this.coverImageFileId = coverImageFileId;
    }

    public static Conversation direct() {
        return new Conversation(ConversationType.DIRECT, null, null, null);
    }

    public static Conversation group(String title, UUID ownerId, UUID coverImageFileId) {
        return new Conversation(ConversationType.GROUP, title, ownerId, coverImageFileId);
    }

    public void updateLastMessage(UUID messageId, LocalDateTime at, String preview) {
        this.lastMessageId = messageId;
        this.lastMessageAt = at;
        this.lastMessagePreview = preview;
    }

    public void updateGroupInfo(String title, UUID coverImageFileId) {
        this.title = title;
        this.coverImageFileId = coverImageFileId;
    }
}