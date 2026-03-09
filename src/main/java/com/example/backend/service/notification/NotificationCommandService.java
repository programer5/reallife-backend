package com.example.backend.service.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.notification.event.NotificationCreatedEvent;
import com.example.backend.monitoring.support.NotificationHealthTracker;
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
    private final NotificationHealthTracker notificationHealthTracker;

    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, String body) {
        createIfNotExists(userId, type, refId, null, body);
    }

    @Transactional
    public void createOrRevive(UUID userId, NotificationType type, UUID refId, String body) {
        createOrRevive(userId, type, refId, null, body);
    }

    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, UUID ref2Id, String body) {
        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndDeletedFalse(userId, type, refId)) {
            return;
        }
        try {
            Notification saved = notificationRepository.save(Notification.create(userId, type, refId, ref2Id, body));
            publishCreated(saved);
        } catch (DataIntegrityViolationException e) {
            log.info("🔁 duplicate notification ignored | userId={} type={} refId={}", userId, type, refId);
        }
    }

    @Transactional
    public void createOrRevive(UUID userId, NotificationType type, UUID refId, UUID ref2Id, String body) {
        Notification n = notificationRepository.findByUserIdAndTypeAndRefId(userId, type, refId)
                .map(existing -> {
                    existing.revive(body);
                    existing.updateRef2Id(ref2Id);
                    return existing;
                })
                .orElseGet(() -> Notification.create(userId, type, refId, ref2Id, body));

        Notification saved = notificationRepository.save(n);
        publishCreated(saved);
    }

    private void publishCreated(Notification saved) {
        notificationHealthTracker.markCreated(saved.getType());

        eventPublisher.publishEvent(new NotificationCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getType(),
                saved.getRefId(),
                saved.getRef2Id(),
                saved.getBody(),
                saved.getCreatedAt()
        ));
    }
}