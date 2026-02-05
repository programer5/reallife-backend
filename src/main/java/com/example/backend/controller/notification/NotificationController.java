package com.example.backend.controller.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.service.notification.NotificationQueryService;
import com.example.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse getNotifications(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return notificationQueryService.getMyNotifications(meId);
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable UUID id, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        notificationService.markAsRead(meId, id);
    }

    // ✅ 추가: 전체 읽음 처리
    @PostMapping("/read-all")
    public ReadAllResponse readAll(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        int updatedCount = notificationService.markAllAsRead(meId);
        return new ReadAllResponse(updatedCount);
    }

    public record ReadAllResponse(int updatedCount) {}
}