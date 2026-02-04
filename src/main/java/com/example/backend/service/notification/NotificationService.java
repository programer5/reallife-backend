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
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse list(UUID meId) {

        List<Notification> notifications =
                notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(meId);

        boolean hasUnread =
                notificationRepository.existsByUserIdAndReadAtIsNullAndDeletedFalse(meId);

        List<NotificationListResponse.Item> items = notifications.stream()
                .map(n -> new NotificationListResponse.Item(
                        n.getId(),
                        n.getType().name(),
                        n.getBody(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .toList();

        return new NotificationListResponse(items, hasUnread);
    }
}