package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_conv_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_conv_id_id", columnList = "conversation_id, id")
        }
)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content; // nullable 가능

    @Column(nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MessageAttachment> attachments = new ArrayList<>();

    private Message(UUID conversationId, UUID senderId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.deleted = false;
    }

    public static Message create(UUID conversationId, UUID senderId, String content) {
        return new Message(conversationId, senderId, content);
    }

    public void addAttachment(String fileKey, String originalName, String mimeType, long sizeBytes) {
        this.attachments.add(MessageAttachment.create(this, fileKey, originalName, mimeType, sizeBytes));
    }
}