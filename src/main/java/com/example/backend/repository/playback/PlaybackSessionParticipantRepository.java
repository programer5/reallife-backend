package com.example.backend.repository.playback;

import com.example.backend.domain.playback.PlaybackSessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackSessionParticipantRepository extends JpaRepository<PlaybackSessionParticipant, UUID> {
    List<PlaybackSessionParticipant> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    Optional<PlaybackSessionParticipant> findBySessionIdAndUserId(UUID sessionId, UUID userId);
}
