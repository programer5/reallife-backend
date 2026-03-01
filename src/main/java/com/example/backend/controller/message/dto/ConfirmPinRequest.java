package com.example.backend.controller.message.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 메시지에서 감지된 후보를 "확정"하여 실제 핀을 생성한다.
 *
 * override 값은 선택 사항이며, null이면 감지값을 그대로 사용한다.
 */
public record ConfirmPinRequest(
        UUID messageId,
        String overrideTitle,
        LocalDateTime overrideStartAt,
        String overridePlaceText,
        Integer overrideRemindMinutes // ✅ NEW
) {}