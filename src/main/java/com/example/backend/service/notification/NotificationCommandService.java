package com.example.backend.service.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.notification.event.NotificationCreatedEvent;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 기존 방식(메시지/좋아요 등): "없으면 생성"
     */
    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, String body) {

        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndDeletedFalse(userId, type, refId)) {
            return;
        }

        try {
            Notification saved = notificationRepository.save(Notification.create(userId, type, refId, body));
            publishCreated(saved);

        } catch (DataIntegrityViolationException e) {
            log.info("🔁 duplicate notification ignored | userId={} type={} refId={}", userId, type, refId);
        }
    }

    /**
     * ✅ 추천: FOLLOW 전용 "되살리기"
     * - 이미 있으면 revive + 다시 이벤트 발행(SSE/화면 갱신용)
     * - 없으면 새로 생성
     */
    @Transactional
    public void createOrRevive(UUID userId, NotificationType type, UUID refId, String body) {
        Notification n = notificationRepository.findByUserIdAndTypeAndRefId(userId, type, refId)
                .map(existing -> {
                    existing.revive(body);
                    return existing;
                })
                .orElseGet(() -> Notification.create(userId, type, refId, body));

        Notification saved = notificationRepository.save(n);
        publishCreated(saved);
    }

    private void publishCreated(Notification saved) {
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getType(),
                saved.getRefId(),
                saved.getBody(),
                saved.getCreatedAt()
        ));
    }
}