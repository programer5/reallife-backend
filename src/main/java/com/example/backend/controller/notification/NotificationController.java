package com.example.backend.controller.notification;

import com.example.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PatchMapping("/{notificationId}/read")
    public void read(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        notificationService.markAsRead(meId, notificationId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        long count = notificationService.countUnread(meId);
        return Map.of("count", count);
    }
}