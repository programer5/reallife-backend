package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);

    long countByReceiverIdAndReadAtIsNull(UUID receiverId);
}