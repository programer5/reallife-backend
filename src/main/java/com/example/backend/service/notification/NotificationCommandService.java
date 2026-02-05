package com.example.backend.service.notification;

import com.example.backend.domain.notification.Notification;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createIfNotExists(UUID userId, NotificationType type, UUID refId, String body) {

        // 1차 방어(빠른 return) - 대부분 여기서 끝남
        if (notificationRepository.existsByUserIdAndTypeAndRefIdAndDeletedFalse(userId, type, refId)) {
            return;
        }

        try {
            // 2차 방어(DB 유니크) - 레이스 상황은 여기서 막힘
            notificationRepository.save(Notification.create(userId, type, refId, body));
        } catch (DataIntegrityViolationException e) {
            // uk_notification_dedupe 충돌이면 "이미 생성됨" 케이스로 보고 무시
            log.info("🔁 duplicate notification ignored | userId={} type={} refId={}", userId, type, refId);
        }
    }
}