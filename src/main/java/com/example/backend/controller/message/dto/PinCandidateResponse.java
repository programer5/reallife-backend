package com.example.backend.controller.message.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지에서 감지된 "핀 후보" (DB 저장 X).
 * 사용자가 확정(Confirm)하면 실제 ConversationPin으로 생성된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinCandidateResponse(
        String candidateId,          // messageId 기반 식별자(클라이언트에서 key용)
        String type,                 // "SCHEDULE"
        String title,                // 기본 "약속"
        String placeText,            // nullable
        LocalDateTime startAt,       // nullable
        LocalDateTime remindAt,      // nullable
        Double confidence,           // nullable
        List<String> reasonTags      // nullable
) {}