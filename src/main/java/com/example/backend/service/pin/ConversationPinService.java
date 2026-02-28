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

        // ✅ 1) 멱등: 같은 메시지 기반 핀이 이미 있으면 그대로 반환 (이 경우 이벤트 발행 X)
        Optional<ConversationPin> existing =
                pinRepository.findByConversationIdAndSourceMessageIdAndDeletedFalse(conversationId, messageId);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // ✅ 2) 메시지에서 일정 후보 파싱
        PinDetectionService.DetectionResult detected = detectionService.detect(messageContent)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));

        String title = (overrideTitle != null && !overrideTitle.isBlank()) ? overrideTitle : detected.title();
        LocalDateTime startAt = (overrideStartAt != null) ? overrideStartAt : detected.startAt();
        String placeText = (overridePlaceText != null && !overridePlaceText.isBlank()) ? overridePlaceText : detected.placeText();

        if (startAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // ✅ 3) 같은 messageId로 중복 저장은 DB 유니크 + 멱등으로 해결하므로
        // "비슷한 일정 중복"을 강제로 막는 로직은 제거하는 게 UX가 좋음.
        // (원하면 나중에 '경고' UX로만 추가하자.)

        ConversationPin pin = ConversationPin.createSchedule(
                conversationId,
                meId,
                messageId,   // ✅ sourceMessageId
                title,
                placeText,
                startAt
        );

        ConversationPin saved;
        try {
            saved = pinRepository.save(pin);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // ✅ 4) 동시성 유니크 충돌 → 이미 생성된 핀을 조회해서 그대로 반환(멱등 보장)
            Optional<ConversationPin> again =
                    pinRepository.findByConversationIdAndSourceMessageIdAndDeletedFalse(conversationId, messageId);

            if (again.isPresent()) {
                return toResponse(again.get());
            }
            throw e;
        }

        // ✅ 5) 새로 생성된 경우에만 이벤트 발행
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

        return toResponse(saved);
    }

    private ConversationPinResponse toResponse(ConversationPin p) {
        return new ConversationPinResponse(
                p.getId(),
                p.getConversationId(),
                p.getCreatedBy(),
                p.getType().name(),
                p.getTitle(),
                p.getPlaceText(),
                p.getStartAt(),
                p.getRemindAt(),
                p.getStatus().name(),
                p.getCreatedAt()
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

    @Transactional(readOnly = true)
    public ConversationPinResponse getPin(UUID meId, UUID pinId) {
        ConversationPin p = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));

        if (p.isDeleted()) {
            throw new BusinessException(ErrorCode.PIN_NOT_FOUND);
        }

        // ✅ 대화방 멤버만 접근 가능
        if (!memberRepository.existsByConversationIdAndUserId(p.getConversationId(), meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return new ConversationPinResponse(
                p.getId(),
                p.getConversationId(),
                p.getCreatedBy(),
                p.getType().name(),
                p.getTitle(),
                p.getPlaceText(),
                p.getStartAt(),
                p.getRemindAt(),
                p.getStatus().name(),
                p.getCreatedAt()
        );
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