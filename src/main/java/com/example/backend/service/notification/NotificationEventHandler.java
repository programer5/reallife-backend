package com.example.backend.service.notification;

import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.message.ConversationParticipantRepository;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final ConversationParticipantRepository participantRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 메시지 전송 → 상대방에게 알림 생성
     * 트랜잭션 커밋 이후에만 실행됨
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {

        UUID conversationId = event.conversationId();
        UUID senderId = event.senderId();

        // 1️⃣ 대화 참가자 조회
        List<UUID> participants =
                participantRepository.findUserIdsByConversationId(conversationId);

        // 2️⃣ 보낸 사람 제외
        participants.stream()
                .filter(userId -> !userId.equals(senderId))
                .forEach(receiverId -> {

                    String body = "새 메시지가 도착했습니다.";

                    Notification notification = Notification.create(
                            receiverId,
                            NotificationType.MESSAGE_RECEIVED,
                            event.messageId(),
                            body
                    );

                    notificationRepository.save(notification);
                });
    }
}