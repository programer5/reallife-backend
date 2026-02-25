package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.*;
import com.example.backend.service.message.ConversationLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}")
public class ConversationLockController {

    private final ConversationLockService lockService;

    @GetMapping("/lock")
    public ConversationLockStatusResponse status(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId
    ) {
        UUID meId = UUID.fromString(userId);
        boolean enabled = lockService.isLockEnabled(conversationId, meId);
        return new ConversationLockStatusResponse(enabled);
    }

    @PostMapping("/lock")
    public ConversationLockStatusResponse enable(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationLockSetRequest req
    ) {
        UUID meId = UUID.fromString(userId);
        lockService.enableLock(conversationId, meId, req.password());
        return new ConversationLockStatusResponse(true);
    }

    @PostMapping("/unlock")
    public ConversationLockStatusResponse disable(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationLockDisableRequest req
    ) {
        UUID meId = UUID.fromString(userId);
        lockService.disableLock(conversationId, meId, req.password());
        return new ConversationLockStatusResponse(false);
    }

    @PostMapping("/unlock-token")
    public ConversationUnlockTokenResponse unlockToken(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationUnlockTokenRequest req
    ) {
        UUID meId = UUID.fromString(userId);
        var issued = lockService.issueUnlockToken(conversationId, meId, req.password());
        return new ConversationUnlockTokenResponse(issued.token(), issued.expiresAt());
    }
}