package com.example.backend.service.sse;

import com.example.backend.domain.message.event.MessageDeletedEvent;
import com.example.backend.domain.message.event.MessageSentEvent;
import com.example.backend.domain.notification.event.NotificationCreatedEvent;
import com.example.backend.domain.pin.event.PinCreatedEvent;
import com.example.backend.domain.pin.event.PinUpdatedEvent;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.sse.SsePushPort;
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

    private final SsePushPort pushService;
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

        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId());

        var payload = Map.of(
                "messageId", event.messageId().toString(),
                "conversationId", event.conversationId().toString(),
                "deletedByUserId", event.deletedByUserId().toString(),
                "deletedAt", event.deletedAt().toString()
        );

        for (UUID targetId : targets) {
            pushService.push(targetId, "message-deleted", payload, event.messageId().toString());
        }
    }

    // ✅ NEW: pin-created (핀은 "나 포함 전원"에게 보내야 UX가 바로 반영됨)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPinCreated(PinCreatedEvent event) {

        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId());

        Map<String, Object> payload = Map.of(
                "pinId", event.pinId().toString(),
                "conversationId", event.conversationId().toString(),
                "createdBy", event.createdBy().toString(),
                "type", event.type(),
                "title", event.title(),
                "placeText", event.placeText(),
                "startAt", event.startAt() == null ? null : event.startAt().toString(),
                "remindAt", event.remindAt() == null ? null : event.remindAt().toString(),
                "status", event.status(),
                "createdAt", event.createdAt().toString()
        );

        for (UUID targetId : targets) {
            pushService.push(targetId, "pin-created", payload, event.pinId().toString());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPinUpdated(PinUpdatedEvent event) {

        List<UUID> targets = memberRepository.findUserIdsByConversationId(event.conversationId());

        Map<String, Object> payload = Map.of(
                "pinId", event.pinId().toString(),
                "conversationId", event.conversationId().toString(),
                "actorId", event.actorId().toString(),
                "action", event.action(),
                "status", event.status(),
                "updatedAt", event.updatedAt().toString()
        );

        for (UUID targetId : targets) {
            pushService.push(targetId, "pin-updated", payload, event.pinId().toString());
        }
    }
}