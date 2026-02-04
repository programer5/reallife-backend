package com.example.backend.domain.notification;

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
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_notification_user_read", columnList = "user_id, read_at")
        }
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // 알림 받을 사람

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "ref_id", nullable = false)
    private UUID refId; // 예: messageId, postId 등

    @Column(name = "body", nullable = false, length = 300)
    private String body; // 화면에 보여줄 텍스트

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    private Notification(UUID userId, NotificationType type, UUID refId, String body) {
        this.userId = userId;
        this.type = type;
        this.refId = refId;
        this.body = body;
        this.deleted = false;
    }

    public static Notification create(UUID userId, NotificationType type, UUID refId, String body) {
        return new Notification(userId, type, refId, body);
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public void delete() {
        this.deleted = true;
    }

    public boolean isRead() {
        return readAt != null;
    }
}