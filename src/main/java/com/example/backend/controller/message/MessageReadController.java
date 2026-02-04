package com.example.backend.controller.message;

import com.example.backend.service.message.MessageReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class MessageReadController {

    private final MessageReadService messageReadService;

    @PatchMapping("/{conversationId}/read")
    public void read(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        messageReadService.markAsRead(meId, conversationId);
    }
}