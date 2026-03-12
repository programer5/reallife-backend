package com.example.backend.controller.message;

import com.example.backend.controller.message.dto.*;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.service.message.ConversationListService;
import com.example.backend.service.message.ConversationService;
import com.example.backend.service.message.MessageReadService;
import jakarta.validation.Valid;
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

    @GetMapping("/{conversationId}/read-receipts")
    public ConversationReadReceiptsResponse readReceipts(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID conversationId
    ) {
        UUID meId = UUID.fromString(userId);

        // 멤버십 체크
        memberRepository.findByConversationIdAndUserId(conversationId, meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_FORBIDDEN));

        var members = memberRepository.findActiveMembers(conversationId);

        var items = members.stream()
                .map(m -> new ConversationReadReceiptsResponse.Item(m.getUserId(), m.getLastReadAt()))
                .toList();

        return new ConversationReadReceiptsResponse(items);
    }

    @PostMapping("/group")
    public GroupConversationCreateResponse createGroup(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GroupConversationCreateRequest req
    ) {
        UUID meId = UUID.fromString(userId);
        UUID conversationId = conversationService.createGroup(
                meId,
                req.title(),
                req.participantIds(),
                req.coverImageFileId()
        );
        return new GroupConversationCreateResponse(conversationId);
    }
}