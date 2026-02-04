package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}