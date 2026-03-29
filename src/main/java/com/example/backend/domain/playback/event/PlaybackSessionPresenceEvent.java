package com.example.backend.domain.playback.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlaybackSessionPresenceEvent(
        UUID sessionId,
        UUID conversationId,
        UUID userId,
        LocalDateTime lastSeenAt
) {
}
