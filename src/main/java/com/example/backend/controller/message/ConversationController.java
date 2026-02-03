package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.ConversationCreateRequest;
import com.example.backend.controller.message.dto.ConversationResponse;
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

    @PostMapping
    public ConversationResponse createOrGet(@RequestBody ConversationCreateRequest request,
                                            Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return conversationService.createOrGetDirect(meId, request.targetUserId());
    }
}