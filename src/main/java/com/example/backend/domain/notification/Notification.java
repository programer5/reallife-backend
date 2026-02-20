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
                @Index(name = "idx_notification_user_read", columnList = "user_id, read_at"),
                @Index(name = "idx_notification_user_deleted", columnList = "user_id, deleted")
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

    private Notification(UUID userId, NotificationType type, UUID refId, String body) {
        this.userId = userId;
        this.type = type;
        this.refId = refId;
        this.body = body;
    }

    public static Notification create(UUID userId, NotificationType type, UUID refId, String body) {
        return new Notification(userId, type, refId, body);
    }

    /** ✅ 멱등 */
    public boolean markAsRead() {
        if (this.readAt != null) return false;
        this.readAt = LocalDateTime.now();
        return true;
    }

    /** ✅ 멱등 + BaseEntity deleted 사용 */
    public boolean delete() {
        if (this.isDeleted()) return false;
        this.markDeleted();
        return true;
    }

    public boolean isRead() {
        return readAt != null;
    }

    /**
     * ✅ "되살리기" (FOLLOW 같은 알림에 필요)
     * - 다시 팔로우하면 다시 알림이 떠야 하므로 readAt 초기화
     * - deleted=true 였어도 복구
     * - body 최신화
     */
    public void revive(String newBody) {
        this.restore();       // BaseEntity: deleted=false 로 복구
        this.readAt = null;   // 미읽음으로 되돌림
        this.body = newBody;  // 메시지 갱신
    }
}