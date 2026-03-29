package com.example.backend.service.playback;

import com.example.backend.controller.playback.dto.PlaybackSessionCreateRequest;
import com.example.backend.controller.playback.dto.PlaybackSessionListResponse;
import com.example.backend.controller.playback.dto.PlaybackSessionResponse;
import com.example.backend.controller.playback.dto.PlaybackSessionStateUpdateRequest;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.Message;
import com.example.backend.domain.playback.*;
import com.example.backend.domain.playback.event.PlaybackSessionCreatedEvent;
import com.example.backend.domain.playback.event.PlaybackSessionEndedEvent;
import com.example.backend.domain.playback.event.PlaybackSessionUpdatedEvent;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.repository.playback.PlaybackSessionParticipantRepository;
import com.example.backend.repository.playback.PlaybackSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaybackSessionService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final PlaybackSessionRepository playbackSessionRepository;
    private final PlaybackSessionParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PlaybackSessionListResponse list(UUID conversationId, UUID meId) {
        ensureConversationMember(conversationId, meId);
        List<PlaybackSessionResponse> items = playbackSessionRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new PlaybackSessionListResponse(items);
    }

    public PlaybackSessionResponse create(UUID conversationId, UUID meId, PlaybackSessionCreateRequest request) {
        Conversation conversation = ensureConversationMember(conversationId, meId);

        PlaybackSession session = playbackSessionRepository.save(PlaybackSession.create(
                conversationId,
                meId,
                request.mediaKind(),
                request.title().trim(),
                request.sourceUrl().trim(),
                blankToNull(request.thumbnailUrl())
        ));

        List<UUID> memberIds = conversationMemberRepository.findUserIdsByConversationId(conversationId);
        for (UUID memberId : memberIds) {
            PlaybackParticipantRole role = memberId.equals(meId) ? PlaybackParticipantRole.HOST : PlaybackParticipantRole.GUEST;
            participantRepository.save(PlaybackSessionParticipant.create(session.getId(), conversationId, memberId, role));
        }

        Message message = messageRepository.save(Message.session(
                conversationId,
                meId,
                sessionSummaryText(session),
                buildSessionMessageMetadata(session),
                session.getId()
        ));
        session.attachMessage(message.getId());

        String preview = sessionSummaryText(session);
        conversation.updateLastMessage(message.getId(), message.getCreatedAt(), preview.length() > 200 ? preview.substring(0, 200) : preview);

        eventPublisher.publishEvent(new PlaybackSessionCreatedEvent(
                session.getId(),
                conversationId,
                meId,
                session.getTitle(),
                session.getMediaKind(),
                session.getSourceUrl(),
                session.getThumbnailUrl(),
                session.getStatus(),
                session.getPlaybackState(),
                session.getPositionSeconds(),
                session.getCreatedAt(),
                message.getId()
        ));

        return toResponse(session);
    }

    public PlaybackSessionResponse updateState(UUID conversationId, UUID sessionId, UUID meId, PlaybackSessionStateUpdateRequest request) {
        ensureConversationMember(conversationId, meId);
        PlaybackSession session = getSession(conversationId, sessionId);
        ensureHost(session, meId);
        ensureActive(session);

        LocalDateTime now = LocalDateTime.now();
        session.updatePlayback(meId, request.playbackState(), request.positionSeconds(), now);
        participantRepository.findBySessionIdAndUserId(sessionId, meId).ifPresent(p -> p.touch(now));

        eventPublisher.publishEvent(new PlaybackSessionUpdatedEvent(
                session.getId(),
                session.getConversationId(),
                meId,
                session.getStatus(),
                session.getPlaybackState(),
                session.getPositionSeconds(),
                now
        ));

        return toResponse(session);
    }

    public PlaybackSessionResponse end(UUID conversationId, UUID sessionId, UUID meId, long positionSeconds) {
        ensureConversationMember(conversationId, meId);
        PlaybackSession session = getSession(conversationId, sessionId);
        ensureHost(session, meId);
        ensureActive(session);

        LocalDateTime now = LocalDateTime.now();
        session.end(meId, positionSeconds, now);
        participantRepository.findBySessionIdAndUserId(sessionId, meId).ifPresent(p -> p.touch(now));

        eventPublisher.publishEvent(new PlaybackSessionEndedEvent(
                session.getId(),
                session.getConversationId(),
                meId,
                session.getPositionSeconds(),
                now
        ));

        return toResponse(session);
    }

    private Conversation ensureConversationMember(UUID conversationId, UUID meId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.MESSAGE_FORBIDDEN);
        }
        return conversation;
    }

    private PlaybackSession getSession(UUID conversationId, UUID sessionId) {
        return playbackSessionRepository.findByIdAndConversationIdAndDeletedFalse(sessionId, conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYBACK_SESSION_NOT_FOUND));
    }

    private void ensureHost(PlaybackSession session, UUID meId) {
        if (!session.getHostUserId().equals(meId)) {
            throw new BusinessException(ErrorCode.PLAYBACK_SESSION_HOST_ONLY);
        }
    }

    private void ensureActive(PlaybackSession session) {
        if (session.getStatus() == PlaybackSessionStatus.ENDED) {
            throw new BusinessException(ErrorCode.PLAYBACK_SESSION_ALREADY_ENDED);
        }
    }

    private PlaybackSessionResponse toResponse(PlaybackSession session) {
        List<PlaybackSessionResponse.Participant> participants = participantRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(participant -> new PlaybackSessionResponse.Participant(
                        participant.getUserId(),
                        participant.getRole(),
                        participant.getLastSeenAt()
                ))
                .toList();

        return new PlaybackSessionResponse(
                session.getId(),
                session.getConversationId(),
                session.getHostUserId(),
                session.getMessageId(),
                session.getMediaKind(),
                session.getTitle(),
                session.getSourceUrl(),
                session.getThumbnailUrl(),
                session.getStatus(),
                session.getPlaybackState(),
                session.getPositionSeconds(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getLastControlledAt(),
                session.getLastControlledBy(),
                session.getCreatedAt(),
                participants
        );
    }

    private String sessionSummaryText(PlaybackSession session) {
        return switch (session.getMediaKind()) {
            case MUSIC -> "같이 듣기 세션 · " + session.getTitle();
            case MOVIE, VIDEO -> "같이 보기 세션 · " + session.getTitle();
            case LINK -> "같이 열기 세션 · " + session.getTitle();
        };
    }

    private String buildSessionMessageMetadata(PlaybackSession session) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "playback-session");
        payload.put("sessionId", session.getId());
        payload.put("mediaKind", session.getMediaKind().name());
        payload.put("title", session.getTitle());
        payload.put("sourceUrl", session.getSourceUrl());
        payload.put("thumbnailUrl", session.getThumbnailUrl());
        payload.put("status", session.getStatus().name());
        payload.put("playbackState", session.getPlaybackState().name());
        payload.put("positionSeconds", session.getPositionSeconds());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
