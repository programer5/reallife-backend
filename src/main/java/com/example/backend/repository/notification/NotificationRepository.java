package com.example.backend.repository.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    // ✅ 최신 50개 조회
    List<Notification> findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);

    // ✅ 미읽음 존재 여부(hasUnread)
    boolean existsByUserIdAndReadAtIsNullAndDeletedFalse(UUID userId);

    // ✅ 단건 권한/존재 확인(read/delete)
    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    // ✅ 중복 생성 방지(1차 방어)
    boolean existsByUserIdAndTypeAndRefIdAndDeletedFalse(UUID userId, NotificationType type, UUID refId);

    // ✅ 추가(가벼운 존재 확인)
    boolean existsByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    /**
     * ✅ FOLLOW 같은 알림 되살리기용
     * deleted 여부 무관하게 한 번이라도 생성된 알림을 찾는다.
     * (DB에 UNIQUE가 걸려있으면, deleted=true여도 insert가 막히는 경우가 있어 revive가 안전함)
     */
    Optional<Notification> findByUserIdAndTypeAndRefId(UUID userId, NotificationType type, UUID refId);
}