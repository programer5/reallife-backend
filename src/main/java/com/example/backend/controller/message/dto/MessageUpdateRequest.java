// MessageUpdateRequest.java
package com.example.backend.controller.message.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageUpdateRequest(
        @NotBlank(message = "내용은 비어 있을 수 없습니다.")
        String content
) {}