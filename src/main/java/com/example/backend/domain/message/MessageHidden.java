package com.example.backend.domain.message;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "message_hidden",
        uniqueConstraints = @UniqueConstraint(name = "uk_message_hidden", columnNames = {"user_id", "message_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHidden {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "message_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID messageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static MessageHidden hide(UUID userId, UUID messageId) {
        MessageHidden mh = new MessageHidden();
        mh.id = UUID.randomUUID();
        mh.userId = userId;
        mh.messageId = messageId;
        mh.createdAt = LocalDateTime.now();
        return mh;
    }
}