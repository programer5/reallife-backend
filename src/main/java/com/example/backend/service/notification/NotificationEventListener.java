package com.example.backend.service.notification;

import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final ConversationParticipantRepository participantRepository;

    @Transactional
    @EventListener
    public void onMessageSent(MessageSentEvent event) {

        // ✅ 대화 참여자 목록(보낸 사람 제외)에게 알림 생성
        List<UUID> targets = participantRepository.findUserIdsByConversationId(event.conversationId())
                .stream()
                .filter(id -> !id.equals(event.senderId()))
                .toList();

        for (UUID targetId : targets) {
            Notification n = Notification.create(
                    targetId,
                    NotificationType.MESSAGE_RECEIVED,
                    event.messageId(),
                    "새 메시지가 도착했습니다."
            );
            notificationRepository.save(n);
        }

        log.info("🔔 notifications created | convId={} targets={}", event.conversationId(), targets.size());
    }
}