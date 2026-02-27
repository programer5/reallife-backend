package com.example.backend.service.pin;

import com.example.backend.controller.message.dto.ConversationPinResponse;
import com.example.backend.controller.message.dto.PinCandidateResponse;
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

    public List<PinCandidateResponse> detectCandidates(UUID messageId, String messageContent) {
        if (messageContent == null || messageContent.isBlank()) return List.of();

        Optional<PinDetectionService.DetectionResult> detected = detectionService.detect(messageContent);
        if (detected.isEmpty()) return List.of();

        PinDetectionService.DetectionResult r = detected.get();

        return List.of(new PinCandidateResponse(
                "msg:" + messageId,
                r.type(),
                r.title(),
                r.placeText(),
                r.startAt(),
                r.remindAt(),
                null,
                List.of("rule_based")
        ));
    }

    @Transactional
    public ConversationPinResponse confirmFromMessage(
            UUID meId,
            UUID conversationId,
            UUID messageId,
            String messageContent,
            String overrideTitle,
            LocalDateTime overrideStartAt,
            String overridePlaceText
    ) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (messageContent == null || messageContent.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // ✅ (1번) 같은 메시지에서 여러 번 저장 방지 (서버에서 확실히 차단)
        // ConversationPin에 sourceMessageId 컬럼 추가 + Repository exists 메서드 추가가 되어 있어야 함
        if (pinRepository.existsByConversationIdAndSourceMessageIdAndDeletedFalse(conversationId, messageId)) {
            throw new BusinessException(ErrorCode.PIN_ALREADY_SAVED);
        }

        PinDetectionService.DetectionResult detected = detectionService.detect(messageContent)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        String title = (overrideTitle != null && !overrideTitle.isBlank()) ? overrideTitle : detected.title();
        LocalDateTime startAt = (overrideStartAt != null) ? overrideStartAt : detected.startAt();
        String placeText = (overridePlaceText != null && !overridePlaceText.isBlank()) ? overridePlaceText : detected.placeText();

        if (startAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Optional<ConversationPin> latest = pinRepository
                .findTopByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(conversationId, PinStatus.ACTIVE);

        if (latest.isPresent()) {
            ConversationPin last = latest.get();
            if (isDuplicateWithinWindow(last, startAt, placeText)) {
                log.info("📌 pin confirm skipped (duplicate) | conversationId={} lastPinId={}", conversationId, last.getId());
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        // ✅ sourceMessageId까지 저장되도록 createSchedule 시그니처가 (conversationId, meId, messageId, ...) 형태여야 함
        ConversationPin pin = ConversationPin.createSchedule(
                conversationId,
                meId,
                messageId,     // ✅ sourceMessageId
                title,
                placeText,
                startAt
        );

        ConversationPin saved = pinRepository.save(pin);

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

        return new ConversationPinResponse(
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
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationPin> listActivePins(UUID conversationId, UUID meId, int size) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<ConversationPin> pins = pinRepository.findByConversationIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
                conversationId,
                PinStatus.ACTIVE,
                PageRequest.of(0, size)
        );

        if (pins.isEmpty()) return pins;

        List<UUID> pinIds = pins.stream().map(ConversationPin::getId).toList();

        // ✅ 너 프로젝트 원본 메서드명: findAllByUserIdAndPinIdIn
        List<ConversationPinDismissal> dismissals = dismissalRepository.findAllByUserIdAndPinIdIn(meId, pinIds);
        List<UUID> dismissedPinIds = dismissals.stream().map(ConversationPinDismissal::getPinId).toList();

        if (dismissedPinIds.isEmpty()) return pins;

        return pins.stream()
                .filter(p -> !dismissedPinIds.contains(p.getId()))
                .toList();
    }

    @Transactional
    public void markDone(UUID meId, UUID pinId) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pin.markDone();

        // ✅ PinUpdatedEvent 시그니처에 맞춤
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
                null,
                pin.getUpdateAt()
        ));
    }

    @Transactional
    public void markCanceled(UUID meId, UUID pinId) {
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
                null,
                pin.getUpdateAt()
        ));
    }

    @Transactional
    public void dismiss(UUID meId, UUID pinId) {
        ConversationPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (!memberRepository.existsByConversationIdAndUserId(pin.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // ✅ 너 프로젝트 원본: findByPinIdAndUserId / existsByPinIdAndUserId / ConversationPinDismissal.of(...)
        dismissalRepository.findByPinIdAndUserId(pinId, meId)
                .orElseGet(() -> dismissalRepository.save(ConversationPinDismissal.of(pinId, meId)));

        eventPublisher.publishEvent(new PinUpdatedEvent(
                pin.getId(),
                pin.getConversationId(),
                meId,
                "DISMISSED",
                pin.getStatus().name(),
                pin.getTitle(),
                pin.getPlaceText(),
                pin.getStartAt(),
                pin.getRemindAt(),
                meId, // dismiss는 per-user
                pin.getUpdateAt()
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
                null,
                pin.getUpdateAt()
        ));
    }

    private boolean isDuplicateWithinWindow(ConversationPin last, LocalDateTime startAt, String placeText) {
        if (last.getStartAt() == null || startAt == null) return false;
        if (Duration.between(last.getCreatedAt(), LocalDateTime.now()).abs().toMinutes() > 2) return false;

        boolean sameStartAt = last.getStartAt().equals(startAt);
        boolean samePlace = (last.getPlaceText() == null && placeText == null)
                || (last.getPlaceText() != null && last.getPlaceText().equals(placeText));

        return sameStartAt && samePlace;
    }
}