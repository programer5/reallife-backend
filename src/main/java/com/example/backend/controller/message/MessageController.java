package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageSendRequest;
import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.service.message.MessageCommandService;
import com.example.backend.service.message.MessageQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}