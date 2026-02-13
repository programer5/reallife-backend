package com.example.backend.controller.message;

import com.example.backend.service.message.MessageDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageDeleteController {

    private final MessageDeleteService deleteService;

    /** 나만 삭제(숨김) */
    @PostMapping("/{messageId}/hide")
    public void hide(@PathVariable UUID messageId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        deleteService.hideForMe(meId, messageId);
    }

    /** 모두 삭제(송신자만) */
    @PostMapping("/{messageId}/delete-for-all")
    public void deleteForAll(@PathVariable UUID messageId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        deleteService.deleteForAll(meId, messageId);
    }
}