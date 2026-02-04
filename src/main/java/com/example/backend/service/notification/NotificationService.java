package com.example.backend.service.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.controller.notification.dto.NotificationUnreadCountResponse;
import com.example.backend.domain.notification.Notification;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID meId, int size) {
        int pageSize = Math.min(Math.max(size, 1), 50);

        var notifications = notificationRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        meId,
                        PageRequest.of(0, pageSize)
                );

        var items = notifications.stream()
                .map(NotificationListResponse.Item::from)
                .toList();

        return new NotificationListResponse(items);
    }

    @Transactional
    public void markAsRead(UUID meId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(meId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse unreadCount(UUID meId) {
        long count = notificationRepository
                .countByUserIdAndReadAtIsNullAndDeletedFalse(meId);

        return new NotificationUnreadCountResponse(count);
    }
}