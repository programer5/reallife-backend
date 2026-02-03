package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageListResponse;
import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.service.message.MessageCommandService;
import com.example.backend.service.message.MessageQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageCommandService commandService;
    private final MessageQueryService queryService;

    @PostMapping
    public MessageSendResponse send(@PathVariable UUID conversationId,
                                    @Valid @RequestBody MessageSendRequest req,
                                    Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return commandService.send(meId, conversationId, req);
    }

    @GetMapping
    public MessageListResponse list(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) UUID cursorMessageId,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return queryService.list(conversationId, meId, cursorCreatedAt, cursorMessageId, size);
    }
}