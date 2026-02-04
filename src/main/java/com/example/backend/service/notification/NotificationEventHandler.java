package com.example.backend.service.notification;

import com.example.backend.domain.notification.NotificationType;
import com.example.backend.service.like.PostLikeService;
import com.example.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationCommandService notificationCommandService;

    /**
     * ✅ 커밋 성공 이후에 알림 생성 (추천)
     * 롤백되면 알림도 생성되면 안되니까.
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikeService.PostLikedEvent e) {
        // TODO: postId로 "작성자" 찾기 필요 -> PostRepository로 조회해서 authorId 얻기
        // 지금은 구조만 잡고, 다음 단계에서 구현하자.
        log.info("post liked event postId={}, userId={}", e.postId(), e.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostUnliked(PostLikeService.PostUnlikedEvent e) {
        // 보통 unlike는 알림 안 만듦(선택)
        log.info("post unliked event postId={}, userId={}", e.postId(), e.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(MessageService.MessageSentEvent e) {
        // TODO: conversationId로 상대방 userId 찾기 필요 (participantRepository로)
        log.info("message sent event messageId={}, senderId={}", e.messageId(), e.senderId());
    }
}