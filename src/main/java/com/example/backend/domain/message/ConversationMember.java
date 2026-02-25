package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "conversation_members")
public class ConversationMember extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    // ✅ NEW
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    // ✅ DM Lock (per-user per-conversation)
    @Column(name = "lock_enabled", nullable = false)
    private boolean lockEnabled = false;

    @Column(name = "lock_password_hash")
    private String lockPasswordHash;

    // 잠금 설정이 바뀌면 이전 unlock token 무효화
    @Column(name = "lock_version")
    private String lockVersion;

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

    public void markReadAt(LocalDateTime at) {
        this.lastReadAt = at;
    }

    public void enableLock(String passwordHash) {
        this.lockEnabled = true;
        this.lockPasswordHash = passwordHash;
        this.lockVersion = UUID.randomUUID().toString();
    }

    public void disableLock() {
        this.lockEnabled = false;
        this.lockPasswordHash = null;
        this.lockVersion = UUID.randomUUID().toString(); // 기존 토큰 무효화
    }
}