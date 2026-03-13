
package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageCapsuleListResponse;
import com.example.backend.service.message.MessageCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        return service.create(messageId, conversationId, userId, title, LocalDateTime.parse(unlockAt));
    }

    @PostMapping("/api/capsules/{capsuleId}/open")
    public void open(@PathVariable UUID capsuleId) {
        service.open(capsuleId);
    }

    @GetMapping("/api/conversations/{conversationId}/capsules")
    public MessageCapsuleListResponse list(@PathVariable UUID conversationId) {
        return service.listByConversation(conversationId);
    }
}
