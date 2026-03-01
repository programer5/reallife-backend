package com.example.backend.controller.pin;

import com.example.backend.controller.message.dto.ConversationPinResponse;
import com.example.backend.controller.pin.dto.ConversationPinUpdateRequest;
import com.example.backend.service.pin.ConversationPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pins/{pinId}")
public class ConversationPinEditController {

    private final ConversationPinService pinService;

    // ✅ NEW: pinId로 핀 단건 조회 (알림 딥링크용)
    @GetMapping
    public ConversationPinResponse get(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID pinId
    ) {
        return pinService.getPin(UUID.fromString(userId), pinId);
    }

    @PatchMapping
    public void update(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID pinId,
            @RequestBody ConversationPinUpdateRequest req
    ) {
        pinService.updatePin(
                UUID.fromString(userId),
                pinId,
                req.title(),
                req.placeText(),
                req.startAt(),
                req.remindMinutes() // ✅ NEW
        );
    }
}