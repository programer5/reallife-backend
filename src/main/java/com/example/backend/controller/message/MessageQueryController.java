package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.service.message.MessageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageQueryController {

    private final MessageQueryService messageQueryService;

    @GetMapping
    public MessageListResponse list(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Conversation-Unlock-Token", required = false) String unlockToken,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return messageQueryService.list(conversationId, meId, cursor, size, unlockToken);
    }
}