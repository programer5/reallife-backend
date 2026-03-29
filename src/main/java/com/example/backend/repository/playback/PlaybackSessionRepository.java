package com.example.backend.repository.playback;

import com.example.backend.domain.playback.PlaybackSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackSessionRepository extends JpaRepository<PlaybackSession, UUID> {
    List<PlaybackSession> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
    Optional<PlaybackSession> findByIdAndConversationIdAndDeletedFalse(UUID sessionId, UUID conversationId);
}
