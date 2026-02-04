package com.example.backend.controller.notification;

import com.example.backend.controller.notification.dto.NotificationListResponse;
import com.example.backend.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public NotificationListResponse list(
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());

        int pageSize = Math.min(Math.max(size, 1), 50);
        var pageable = PageRequest.of(0, pageSize);

        var items = notificationRepository
                .findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(meId, pageable)
                .stream()
                .map(n -> new NotificationListResponse.Item(
                        n.getId(),
                        n.getType().name(),
                        n.getRefId(),
                        n.getBody(),
                        n.isRead(),
                        n.getCreatedAt()
                ))
                .toList();

        return new NotificationListResponse(items);
    }
}