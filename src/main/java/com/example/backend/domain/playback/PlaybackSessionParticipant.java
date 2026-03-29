package com.example.backend.domain.playback;

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
@Table(name = "playback_session_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_playback_session_participant", columnNames = {"session_id", "user_id"})
})
public class PlaybackSessionParticipant extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private PlaybackParticipantRole role;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    private PlaybackSessionParticipant(UUID sessionId, UUID conversationId, UUID userId, PlaybackParticipantRole role) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
    }

    public static PlaybackSessionParticipant create(UUID sessionId, UUID conversationId, UUID userId, PlaybackParticipantRole role) {
        return new PlaybackSessionParticipant(sessionId, conversationId, userId, role);
    }

    public void touch(LocalDateTime now) {
        this.lastSeenAt = now;
    }
}
