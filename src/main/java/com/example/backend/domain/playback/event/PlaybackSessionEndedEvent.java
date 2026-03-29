package com.example.backend.domain.playback.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlaybackSessionEndedEvent(
        UUID sessionId,
        UUID conversationId,
        UUID actorId,
        long positionSeconds,
        LocalDateTime endedAt
) {
}
