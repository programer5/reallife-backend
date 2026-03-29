package com.example.backend.domain.playback.event;

import com.example.backend.domain.playback.PlaybackSessionStatus;
import com.example.backend.domain.playback.PlaybackState;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlaybackSessionUpdatedEvent(
        UUID sessionId,
        UUID conversationId,
        UUID actorId,
        PlaybackSessionStatus status,
        PlaybackState playbackState,
        long positionSeconds,
        LocalDateTime updatedAt
) {
}
