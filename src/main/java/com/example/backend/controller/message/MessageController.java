package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.MessageSendResponse;
import com.example.backend.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageSendResponse send(
            @PathVariable UUID conversationId,
            @RequestPart(required = false) String content,
            @RequestPart(required = false) List<MultipartFile> files,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return messageService.send(meId, conversationId, content, files);
    }
}