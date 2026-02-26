package com.example.backend.controller.pin;

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

    @PatchMapping
    public void update(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID pinId,
            @RequestBody ConversationPinUpdateRequest req
    ) {
        pinService.updatePlaceText(UUID.fromString(userId), pinId, req.placeText());
    }
}