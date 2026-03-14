
package com.example.backend.controller.message.dto;

import java.util.List;
import java.util.UUID;

public record GroupConversationMemberManageRequest(
        List<UUID> participantIds
) {}
