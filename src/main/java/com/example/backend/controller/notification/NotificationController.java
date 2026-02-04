package com.example.backend.controller.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.controller.notification.dto.NotificationUnreadCountResponse;
import com.example.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse list(
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return notificationService.list(meId, size);
    }

    @PostMapping("/{notificationId}/read")
    public void markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        notificationService.markAsRead(meId, notificationId);
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse unreadCount(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return notificationService.unreadCount(meId);
    }
}