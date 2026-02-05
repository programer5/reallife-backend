package com.example.backend.service.notification;

import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void markAsRead(UUID meId, UUID notificationId) {
        int updated = notificationRepository.markAsReadIfUnread(notificationId, meId, LocalDateTime.now());

        if (updated == 1) return;

        // 이미 읽음이면 OK, 진짜 없으면 NOT_FOUND
        boolean exists = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, meId).isPresent();
        if (!exists) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public int markAllAsRead(UUID meId) {
        return notificationRepository.markAllAsRead(meId, LocalDateTime.now());
    }
}