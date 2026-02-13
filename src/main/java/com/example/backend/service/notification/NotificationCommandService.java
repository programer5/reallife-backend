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

    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, String body) {

        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndDeletedFalse(userId, type, refId)) {
            return;
        }

        try {
            Notification saved = notificationRepository.save(Notification.create(userId, type, refId, body));

            // ✅ 저장 성공했을 때만 이벤트 발행
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    saved.getId(),
                    saved.getUserId(),
                    saved.getType(),
                    saved.getRefId(),
                    saved.getBody(),
                    saved.getCreatedAt()
            ));

        } catch (DataIntegrityViolationException e) {
            log.info("🔁 duplicate notification ignored | userId={} type={} refId={}", userId, type, refId);
        }
    }
}