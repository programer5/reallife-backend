package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    List<Notification> findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    boolean existsByUserIdAndTypeAndRefIdAndDeletedFalse(UUID userId, NotificationType type, UUID refId);

    boolean existsByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Optional<Notification> findByUserIdAndTypeAndRefId(UUID userId, NotificationType type, UUID refId);

    long countByDeletedFalse();
    long countByReadAtIsNullAndDeletedFalse();
    long countByDeletedFalseAndCreatedAtAfter(LocalDateTime createdAt);
    List<Notification> findTop10ByDeletedFalseOrderByCreatedAtDesc();
}