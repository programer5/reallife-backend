package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    // ✅ 사용자 알림 최신순 조회
    List<Notification> findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    // ✅ 미읽음 존재 여부
    boolean existsByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

    // ✅ 단건 조회
    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // ✅ 중복 생성 방지
    boolean existsByUserIdAndTypeAndRefIdAndDeletedFalse(UUID userId, NotificationType type, UUID refId);

    // ✅ 존재 확인
    boolean existsByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // ✅ revive 용
    Optional<Notification> findByUserIdAndTypeAndRefId(UUID userId, NotificationType type, UUID refId);

    // =========================
    // Admin Dashboard 집계용
    // =========================

    long countByDeletedFalse();

    long countByReadAtIsNullAndDeletedFalse();

    long countByDeletedFalseAndCreatedAtAfter(LocalDateTime createdAt);

    List<Notification> findTop10ByDeletedFalseOrderByCreatedAtDesc();
}