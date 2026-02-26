package com.example.backend.service.pin;

import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.pin.PinStatus;
import com.example.backend.domain.pin.event.PinCreatedEvent;
import com.example.backend.repository.pin.ConversationPinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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

    public List<ConversationPin> listActivePins(UUID conversationId, int size) {
        return pinRepository.findByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
                conversationId,
                PinStatus.ACTIVE,
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