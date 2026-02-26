package com.example.backend.controller.pin;

import com.example.backend.service.pin.ConversationPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pins/{pinId}")
public class ConversationPinActionController {

    private final ConversationPinService pinService;

    @PostMapping("/done")
    public void done(@AuthenticationPrincipal String userId, @PathVariable UUID pinId) {
        pinService.markDone(UUID.fromString(userId), pinId);
    }

    @PostMapping("/cancel")
    public void cancel(@AuthenticationPrincipal String userId, @PathVariable UUID pinId) {
        pinService.cancel(UUID.fromString(userId), pinId);
    }

    @PostMapping("/dismiss")
    public void dismiss(@AuthenticationPrincipal String userId, @PathVariable UUID pinId) {
        pinService.dismiss(UUID.fromString(userId), pinId);
    }
}