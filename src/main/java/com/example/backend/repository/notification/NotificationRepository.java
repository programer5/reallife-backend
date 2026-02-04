package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );

    long countByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

    boolean existsByUserIdAndTypeAndRefIdAndDeletedFalse(UUID userId, NotificationType type, UUID refId);

    List<Notification> findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

}