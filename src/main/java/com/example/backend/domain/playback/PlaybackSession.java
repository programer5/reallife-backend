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
@Table(name = "playback_sessions", indexes = {
        @Index(name = "idx_playback_session_conversation_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_playback_session_conversation_status", columnList = "conversation_id, status")
})
public class PlaybackSession extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "host_user_id", nullable = false)
    private UUID hostUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false, length = 20)
    private PlaybackMediaKind mediaKind;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlaybackSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "playback_state", nullable = false, length = 20)
    private PlaybackState playbackState;

    @Column(name = "position_seconds", nullable = false)
    private long positionSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_controlled_at")
    private LocalDateTime lastControlledAt;

    @Column(name = "last_controlled_by")
    private UUID lastControlledBy;

    @Column(name = "message_id")
    private UUID messageId;

    private PlaybackSession(
            UUID conversationId,
            UUID hostUserId,
            PlaybackMediaKind mediaKind,
            String title,
            String sourceUrl,
            String thumbnailUrl
    ) {
        this.conversationId = conversationId;
        this.hostUserId = hostUserId;
        this.mediaKind = mediaKind;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.status = PlaybackSessionStatus.ACTIVE;
        this.playbackState = PlaybackState.PAUSED;
        this.positionSeconds = 0L;
    }

    public static PlaybackSession create(
            UUID conversationId,
            UUID hostUserId,
            PlaybackMediaKind mediaKind,
            String title,
            String sourceUrl,
            String thumbnailUrl
    ) {
        return new PlaybackSession(conversationId, hostUserId, mediaKind, title, sourceUrl, thumbnailUrl);
    }

    public void attachMessage(UUID messageId) {
        this.messageId = messageId;
    }

    public void updatePlayback(UUID actorId, PlaybackState playbackState, long positionSeconds, LocalDateTime now) {
        this.playbackState = playbackState;
        this.positionSeconds = Math.max(0L, positionSeconds);
        this.lastControlledBy = actorId;
        this.lastControlledAt = now;
        if (playbackState == PlaybackState.PLAYING && this.startedAt == null) {
            this.startedAt = now;
        }
    }

    public void end(UUID actorId, long positionSeconds, LocalDateTime now) {
        this.status = PlaybackSessionStatus.ENDED;
        this.playbackState = PlaybackState.PAUSED;
        this.positionSeconds = Math.max(0L, positionSeconds);
        this.endedAt = now;
        this.lastControlledAt = now;
        this.lastControlledBy = actorId;
    }
}
