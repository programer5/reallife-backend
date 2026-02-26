package com.example.backend.service.pin;

import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.pin.ConversationPinDismissal;
import com.example.backend.domain.pin.PinStatus;
import com.example.backend.domain.pin.event.PinCreatedEvent;
import com.example.backend.domain.pin.event.PinUpdatedEvent;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.pin.ConversationPinDismissalRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationPinService {

    private final ConversationPinRepository pinRepository;
    private final PinDetectionService detectionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ConversationPinDismissalRepository dismissalRepository;
    private final ConversationMemberRepository memberRepository;

    public Optional<ConversationPin> tryDetectAndCreateFromMessage(UUID meId, UUID conversationId, String messageContent) {
        if (messageContent == null || messageContent.isBlank()) return Optional.empty();

        Optional<PinDetectionService.DetectionResult> detected = detectionService.detect(messageContent);
        if (detected.isEmpty()) return Optional.empty();

        PinDetectionService.DetectionResult r = detected.get();

        // ✅ 간단 중복 방지: 직전 ACTIVE 핀이 2분 이내 + (startAt/place 동일)하면 스킵
        Optional<ConversationPin> latest = pinRepository
                .findTopByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(conversationId, PinStatus.ACTIVE);

        if (latest.isPresent()) {
            ConversationPin last = latest.get();
            if (isDuplicateWithinWindow(last, r.startAt(), r.placeText())) {
                log.info("📌 pin skipped (duplicate) | conversationId={} lastPinId={}", conversationId, last.getId());
                return Optional.empty();
            }
        }

        ConversationPin pin = ConversationPin.createSchedule(
                conversationId,
                meId,
                r.title(),
                r.placeText(),
                r.startAt()
        );

        ConversationPin saved = pinRepository.save(pin);

        // ✅ SSE용 이벤트 (AFTER_COMMIT에서 broadcast)
        eventPublisher.publishEvent(new PinCreatedEvent(
                saved.getId(),
                saved.getConversationId(),
                saved.getCreatedBy(),
                saved.getType().name(),
                saved.getTitle(),
                saved.getPlaceText(),
                saved.getStartAt(),
                saved.getRemindAt(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        ));

        log.info("📌 PinCreatedEvent published | pinId={} conversationId={}", saved.getId(), conversationId);
        return Optional.of(saved);
    }

    @Transactional
    public void markDone(UUID meId, UUID pinId) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pin.markDone();

        eventPublisher.publishEvent(new PinUpdatedEvent(
                pin.getId(),
                pin.getConversationId(),
                meId,
                "DONE",
                pin.getStatus().name(),
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                null, // broadcast
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void cancel(UUID meId, UUID pinId) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pin.cancel();

        eventPublisher.publishEvent(new PinUpdatedEvent(
                pin.getId(),
                pin.getConversationId(),
                meId,
                "CANCELED",
                pin.getStatus().name(),
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                null, // broadcast
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void dismiss(UUID meId, UUID pinId) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (dismissalRepository.existsByPinIdAndUserId(pinId, meId)) return;
        dismissalRepository.save(ConversationPinDismissal.of(pinId, meId));

        // ✅ dismiss는 "나만" 숨김이므로 targetUserId 지정
        eventPublisher.publishEvent(new PinUpdatedEvent(
                pin.getId(),
                pin.getConversationId(),
                meId,
                "DISMISSED",
                pin.getStatus().name(), // 보통 ACTIVE 유지
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                meId, // only me
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void updatePlaceText(UUID meId, UUID pinId, String placeText) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pin.updatePlaceText(placeText);

        eventPublisher.publishEvent(new PinUpdatedEvent(
                pin.getId(),
                pin.getConversationId(),
                meId,
                "UPDATED",
                pin.getStatus().name(),
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                null, // broadcast (다 같이 보는 핀이라면 broadcast가 맞음)
                LocalDateTime.now()
        ));
    }

    public List<ConversationPin> listActivePins(UUID conversationId, UUID userId, int size) {

        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return pinRepository.findActivePinsVisibleToUser(
                conversationId,
                userId,
                PageRequest.of(0, Math.max(1, Math.min(size, 50)))
        );
    }

    private boolean isDuplicateWithinWindow(ConversationPin last, LocalDateTime startAt, String placeText) {
        LocalDateTime lastCreated = last.getCreatedAt();
        if (lastCreated == null) return false;

        Duration d = Duration.between(lastCreated, LocalDateTime.now());
        if (d.toMinutes() > 2) return false;

        boolean sameStart =
                (last.getStartAt() == null && startAt == null) ||
                        (last.getStartAt() != null && last.getStartAt().equals(startAt));

        boolean samePlace =
                (last.getPlaceText() == null && (placeText == null || placeText.isBlank())) ||
                        (last.getPlaceText() != null && last.getPlaceText().equals(placeText));

        return sameStart && samePlace;
    }
}