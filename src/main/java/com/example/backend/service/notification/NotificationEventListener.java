package com.example.backend.service.notification;

import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.message.ConversationMemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final ConversationMemberRepository memberRepository;
    private final NotificationCommandService notificationCommandService;

    @PostConstruct
    public void init() {
        log.info("✅ NotificationEventListener BEAN CREATED");
    }

    /**
     * 메시지 저장 트랜잭션이 커밋된 이후에만 실행
     * 알림 저장은 별도 새 트랜잭션으로 실행
     *
     * ✅ 중요:
     * - MESSAGE_RECEIVED 알림의 refId는 "conversationId"로 사용 (프론트에서 /chat/{id}로 이동하기 위함)
     * - 같은 대화방에서 새 메시지가 올 때마다 알림이 다시 떠야 하므로 createOrRevive 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {

        log.info("🚨 NotificationEventListener CALLED | messageId={} convId={}",
                event.messageId(), event.conversationId());

        List<UUID> targets = memberRepository
                .findUserIdsByConversationId(event.conversationId())
                .stream()
                .filter(id -> !id.equals(event.senderId()))
                .toList();

        for (UUID targetId : targets) {
            notificationCommandService.createOrRevive(
                    targetId,
                    NotificationType.MESSAGE_RECEIVED,
                    event.conversationId(),  // refId = conversationId (기존 유지)
                    event.messageId(),       // ref2Id = messageId (✅ 추가)
                    "새 메시지가 도착했습니다."
            );
        }

        log.info("🔔 notifications upserted | convId={} targets={}",
                event.conversationId(), targets.size());
    }
}
