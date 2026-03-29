package com.example.backend.domain.playback.event;

import com.example.backend.domain.playback.PlaybackMediaKind;
import com.example.backend.domain.playback.PlaybackSessionStatus;
import com.example.backend.domain.playback.PlaybackState;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlaybackSessionCreatedEvent(
        UUID sessionId,
        UUID conversationId,
        UUID actorId,
        String title,
        PlaybackMediaKind mediaKind,
        String sourceUrl,
        String thumbnailUrl,
        PlaybackSessionStatus status,
        PlaybackState playbackState,
        long positionSeconds,
        LocalDateTime createdAt,
        UUID messageId
) {
}
