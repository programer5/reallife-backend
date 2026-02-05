package com.example.backend.service.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.domain.notification.Notification;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(UUID meId) {

        List<Notification> notifications =
                notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(meId);

        boolean hasUnread =
                notificationRepository.existsByUserIdAndReadAtIsNullAndDeletedFalse(meId);

        return NotificationListResponse.from(notifications, hasUnread);
    }
}