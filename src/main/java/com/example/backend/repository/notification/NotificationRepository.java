package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    // ✅ 목록 조회 (현재 요구사항: 최신 50개)
    List<Notification> findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    // ✅ 미읽음 존재 여부 (hasUnread)
    boolean existsByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

    // ✅ 단건 권한/존재 확인 (read/delete 등에 사용)
    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // ✅ 중복 생성 방지(1차 방어)
    boolean existsByUserIdAndTypeAndRefIdAndDeletedFalse(UUID userId, NotificationType type, UUID refId);
}