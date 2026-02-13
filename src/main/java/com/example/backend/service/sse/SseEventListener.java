package com.example.backend.service.sse;

import com.example.backend.domain.message.event.MessageDeletedEvent;
import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.domain.notification.event.NotificationCreatedEvent;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.sse.SsePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventListener {

    private final SsePushService pushService;
    private final ConversationMemberRepository memberRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageSentEvent event) {

        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId())
                .stream()
                .filter(id -> !id.equals(event.senderId()))
                .toList();

        Map<String, Object> payload = Map.of(
                "messageId", event.messageId().toString(),
                "conversationId", event.conversationId().toString(),
                "senderId", event.senderId().toString(),
                "content", event.content(),
                "createdAt", event.createdAt().toString()
        );

        for (UUID targetId : targets) {
            pushService.push(targetId, "message-created", payload, event.messageId().toString());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {

        Map<String, Object> payload = Map.of(
                "notificationId", event.notificationId().toString(),
                "type", event.type().name(),
                "refId", event.refId().toString(),
                "body", event.body(),
                "createdAt", event.createdAt().toString()
        );

        pushService.push(event.userId(), "notification-created", payload, event.notificationId().toString());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageDeleted(MessageDeletedEvent event) {

        // 대화방 멤버 전체에게 "message-deleted" push
        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId());

        var payload = Map.of(
                "messageId", event.messageId().toString(),
                "conversationId", event.conversationId().toString(),
                "deletedByUserId", event.deletedByUserId().toString(),
                "deletedAt", event.deletedAt().toString()
        );

        for (UUID targetId : targets) {
            // ✅ messageId를 eventId로 사용 (replay에도 유리)
            pushService.push(targetId, "message-deleted", payload, event.messageId().toString());
        }
    }
}