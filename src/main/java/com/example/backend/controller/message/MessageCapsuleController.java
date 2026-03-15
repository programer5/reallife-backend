package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageCapsuleListResponse;
import com.example.backend.service.message.MessageCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageCapsuleController {

    private final MessageCapsuleService service;

    @PostMapping("/api/capsules")
    public UUID create(@RequestParam UUID messageId,
                       @RequestParam UUID conversationId,
                       @RequestParam String title,
                       @RequestParam String unlockAt,
                       @RequestParam UUID userId) {
        return service.create(messageId, conversationId, userId, title, parseUnlockAt(unlockAt));
    }

    @PostMapping("/api/capsules/{capsuleId}/open")
    public void open(@PathVariable UUID capsuleId) {
        service.open(capsuleId);
    }

    @DeleteMapping("/api/capsules/{capsuleId}")
    public void delete(@PathVariable UUID capsuleId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        service.delete(capsuleId, meId);
    }

    @GetMapping("/api/conversations/{conversationId}/capsules")
    public MessageCapsuleListResponse list(@PathVariable UUID conversationId) {
        return service.listByConversation(conversationId);
    }


    private LocalDateTime parseUnlockAt(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("unlockAt is blank");
        }

        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        try {
            return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (Exception ignored) {}

        try {
            return Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {}

        throw new IllegalArgumentException("Unsupported unlockAt format: " + raw);
    }
}
