package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.ConversationListResponse;
import com.example.backend.controller.message.dto.ConversationReadStateResponse;
import com.example.backend.controller.message.dto.DirectConversationCreateRequest;
import com.example.backend.controller.message.dto.DirectConversationCreateResponse;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.service.message.ConversationListService;
import com.example.backend.service.message.ConversationService;
import com.example.backend.service.message.MessageReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationListService listService;
    private final MessageReadService messageReadService;
    private final ConversationService conversationService;
    private final ConversationMemberRepository memberRepository;

    @GetMapping
    public ConversationListResponse list(
            @AuthenticationPrincipal String userId,   // ✅ JwtAuthenticationFilter에서 principal=userId(String)
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID meId = UUID.fromString(userId);
        return listService.list(meId, cursor, size);
    }

    @PostMapping("/{conversationId}/read")
    public void markAsRead(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId
    ) {
        messageReadService.markAsRead(UUID.fromString(userId), conversationId);
    }

    @PostMapping("/direct")
    public DirectConversationCreateResponse createOrGetDirect(
            @AuthenticationPrincipal String userId,
            @RequestBody DirectConversationCreateRequest req
    ) {
        UUID meId = UUID.fromString(userId);
        UUID conversationId = conversationService.createOrGetDirect(meId, req.targetUserId());
        return new DirectConversationCreateResponse(conversationId);
    }

    @GetMapping("/{conversationId}/read-state")
    public ConversationReadStateResponse readState(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId
    ) {
        UUID meId = UUID.fromString(userId);

        ConversationMember member = memberRepository.findByConversationIdAndUserId(conversationId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        return new ConversationReadStateResponse(member.getLastReadAt());
    }
}