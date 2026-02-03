package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.DirectConversationResponse;
import com.example.backend.service.message.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/direct/{targetUserId}")
    public DirectConversationResponse direct(@PathVariable UUID targetUserId,
                                             Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        UUID conversationId = conversationService.createOrGetDirect(meId, targetUserId);
        return new DirectConversationResponse(conversationId);
    }
}