package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.ConversationPinResponse;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.service.pin.ConversationPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/pins")
public class ConversationPinController {

    private final ConversationPinService pinService;

    @GetMapping
    public List<ConversationPinResponse> list(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "10") int size
    ) {
        // MVP: 권한 체크는 ConversationMember 기반이 더 안전하지만
        // 기존 컨트롤러들과 통일하려면 Service에서 member 체크 붙여도 됨.
        UUID meId = UUID.fromString(userId);

        List<ConversationPin> pins = pinService.listActivePins(conversationId, size);

        return pins.stream()
                .map(p -> new ConversationPinResponse(
                        p.getId(),
                        p.getConversationId(),
                        p.getCreatedBy(),
                        p.getType().name(),
                        p.getTitle(),
                        p.getPlaceText(),
                        p.getStartAt(),
                        p.getRemindAt(),
                        p.getStatus().name(),
                        p.getCreatedAt()
                ))
                .toList();
    }
}