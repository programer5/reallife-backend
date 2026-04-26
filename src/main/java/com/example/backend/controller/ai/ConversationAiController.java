package com.example.backend.controller.ai;

import com.example.backend.controller.ai.dto.AiActionExecuteRequest;
import com.example.backend.controller.ai.dto.AiActionExecuteResponse;
import com.example.backend.controller.ai.dto.AiReplyRequest;
import com.example.backend.controller.ai.dto.AiReplyResponse;
import com.example.backend.service.ai.ConversationAiActionService;
import com.example.backend.service.ai.ConversationAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class ConversationAiController {

    private final ConversationAiService conversationAiService;
    private final ConversationAiActionService conversationAiActionService;

    @PostMapping("/reply")
    public AiReplyResponse reply(@RequestBody AiReplyRequest request) {
        return conversationAiService.suggest(request.text());
    }

    @PostMapping("/actions/execute")
    public AiActionExecuteResponse executeAction(
            @AuthenticationPrincipal String userId,
            @RequestBody AiActionExecuteRequest request
    ) {
        return conversationAiActionService.execute(UUID.fromString(userId), request);
    }
}
