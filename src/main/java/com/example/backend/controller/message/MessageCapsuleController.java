package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageCapsuleCreateResponse;
import com.example.backend.controller.message.dto.MessageCapsuleListResponse;
import com.example.backend.controller.message.dto.MessageCapsuleUpdateRequest;
import com.example.backend.service.message.MessageCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageCapsuleController {

    private final MessageCapsuleService service;

    @PostMapping("/api/capsules")
    public MessageCapsuleCreateResponse create(@AuthenticationPrincipal String userId,
                       @RequestParam UUID messageId,
                       @RequestParam UUID conversationId,
                       @RequestParam String title,
                       @RequestParam String unlockAt) {
        UUID meId = UUID.fromString(userId);
        return new MessageCapsuleCreateResponse(service.create(messageId, conversationId, meId, title, parseUnlockAt(unlockAt)));
    }

    @PostMapping("/api/capsules/{capsuleId}/open")
    public void open(@AuthenticationPrincipal String userId,
                     @PathVariable UUID capsuleId) {
        UUID meId = UUID.fromString(userId);
        service.open(capsuleId, meId);
    }

    @GetMapping("/api/conversations/{conversationId}/capsules")
    public MessageCapsuleListResponse list(@AuthenticationPrincipal String userId,
                                           @PathVariable UUID conversationId) {
        UUID meId = UUID.fromString(userId);
        return service.listByConversation(conversationId, meId);
    }

    @PatchMapping("/api/capsules/{capsuleId}")
    public void update(@PathVariable UUID capsuleId,
                       @RequestBody MessageCapsuleUpdateRequest request,
                       @AuthenticationPrincipal String userId) {
        UUID meId = UUID.fromString(userId);
        service.update(capsuleId, meId, request.title(), parseOptionalUnlockAt(request.unlockAt()));
    }

    @DeleteMapping("/api/capsules/{capsuleId}")
    public void delete(@PathVariable UUID capsuleId,
                       @AuthenticationPrincipal String userId) {
        UUID meId = UUID.fromString(userId);
        service.delete(capsuleId, meId);
    }

    private LocalDateTime parseOptionalUnlockAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseUnlockAt(raw);
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
