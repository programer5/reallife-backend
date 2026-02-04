package com.example.backend.service.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, String body) {
        // ✅ 중복 방지 (원치 않으면 삭제 가능)
        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndDeletedFalse(userId, type, refId)) {
            return;
        }

        notificationRepository.save(Notification.create(userId, type, refId, body));
    }
}