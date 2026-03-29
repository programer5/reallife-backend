package com.example.backend.controller.playback.dto;

import com.example.backend.domain.playback.PlaybackMediaKind;
import com.example.backend.domain.playback.PlaybackParticipantRole;
import com.example.backend.domain.playback.PlaybackSessionStatus;
import com.example.backend.domain.playback.PlaybackState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PlaybackSessionResponse(
        UUID sessionId,
        UUID conversationId,
        UUID hostUserId,
        UUID messageId,
        PlaybackMediaKind mediaKind,
        String title,
        String sourceUrl,
        String thumbnailUrl,
        PlaybackSessionStatus status,
        PlaybackState playbackState,
        long positionSeconds,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime lastControlledAt,
        UUID lastControlledBy,
        LocalDateTime createdAt,
        List<Participant> participants
) {
    public record Participant(
            UUID userId,
            PlaybackParticipantRole role,
            LocalDateTime lastSeenAt
    ) {
    }
}
