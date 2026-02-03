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
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_file", columnNames = {"message_id", "file_id"})
        },
        indexes = {
                @Index(name = "idx_message_attachment_message", columnList = "message_id")
        }
)
public class MessageAttachment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    private MessageAttachment(UUID messageId, UUID fileId, int sortOrder) {
        this.messageId = messageId;
        this.fileId = fileId;
        this.sortOrder = sortOrder;
    }

    public static MessageAttachment create(UUID messageId, UUID fileId, int sortOrder) {
        return new MessageAttachment(messageId, fileId, sortOrder);
    }
}
