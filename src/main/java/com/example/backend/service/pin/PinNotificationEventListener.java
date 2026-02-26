package com.example.backend.service.pin;

import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.pin.event.PinCreatedEvent;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.service.notification.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PinNotificationEventListener {

    private final ConversationMemberRepository memberRepository;
    private final NotificationCommandService notificationCommandService;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPinCreated(PinCreatedEvent event) {

        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId())
                .stream()
                // ✅ 핀 만든 사람 제외(원하면 포함으로 바꿔도 됨)
                .filter(id -> !id.equals(event.createdBy()))
                .toList();

        String when = (event.startAt() == null) ? "시간 미정" : DT.format(event.startAt());
        String place = (event.placeText() == null || event.placeText().isBlank()) ? "장소 미정" : event.placeText();

        String body = "📌 약속 핀이 생성됐어요: " + place + " · " + when;

        for (UUID userId : targets) {
            // ✅ refId = pinId 로 중복 방지
            notificationCommandService.createIfNotExists(
                    userId,
                    NotificationType.PIN_CREATED,
                    event.pinId(),
                    body
            );
        }

        log.info("🔔 PIN_CREATED notifications created | pinId={} targets={}", event.pinId(), targets.size());
    }
}