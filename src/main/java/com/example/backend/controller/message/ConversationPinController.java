package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.ConversationPinResponse;
import com.example.backend.controller.message.dto.ConfirmPinRequest;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.service.pin.ConversationPinService;
import com.example.backend.repository.message.MessageRepository;
import com.example.backend.domain.message.Message;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
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
    private final MessageRepository messageRepository;

    @GetMapping
    public List<ConversationPinResponse> list(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID meId = UUID.fromString(userId);
        List<ConversationPin> pins = pinService.listActivePins(conversationId, meId, size);

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

    @PostMapping("/confirm")
    public ConversationPinResponse confirm(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId,
            @RequestBody ConfirmPinRequest req
    ) {
        UUID meId = UUID.fromString(userId);

        Message message = messageRepository.findById(req.messageId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!conversationId.equals(message.getConversationId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return pinService.confirmFromMessage(
                meId,
                conversationId,
                message.getId(),
                message.getContent(),
                req.overrideTitle(),
                req.overrideStartAt(),
                req.overridePlaceText()
        );
    }
}