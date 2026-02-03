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
        name = "message_attachments",
        indexes = {
                @Index(name = "idx_message_id", columnList = "message_id")
        }
)
public class MessageAttachment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "file_key", nullable = false, length = 300)
    private String fileKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private MessageAttachment(Message message, String fileKey, String originalName, String mimeType, long sizeBytes) {
        this.message = message;
        this.fileKey = fileKey;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public static MessageAttachment create(Message message, String fileKey, String originalName, String mimeType, long sizeBytes) {
        return new MessageAttachment(message, fileKey, originalName, mimeType, sizeBytes);
    }
}