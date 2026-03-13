
package com.example.backend.controller.message.dto;

import java.util.List;
import java.util.UUID;

public record GroupConversationMembersResponse(
        UUID conversationId,
        List<MemberItem> items
) {
    public record MemberItem(
            UUID userId,
            String handle,
            String nickname,
            String profileImageUrl
    ) {}
}
