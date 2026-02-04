package com.example.backend.controller.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse list(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return notificationService.list(meId);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        notificationService.markRead(meId, notificationId);
        return ResponseEntity.noContent().build(); // 204
    }
}