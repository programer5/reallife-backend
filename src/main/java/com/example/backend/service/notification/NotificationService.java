package com.example.backend.service.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void markAsRead(UUID meId, UUID notificationId) {
        Notification notification = notificationRepository
                .findByIdAndReceiverId(notificationId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // ✅ 멱등 처리
        if (notification.isRead()) {
            return;
        }

        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID meId) {
        return notificationRepository.countByReceiverIdAndReadAtIsNull(meId);
    }
}