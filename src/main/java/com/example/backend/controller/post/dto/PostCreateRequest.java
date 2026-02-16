package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PostCreateRequest(
        @NotBlank
        @Size(max = 2000, message = "content는 최대 2000자까지 가능합니다.")
        String content,

        // 기존 호환 (프론트 아직 안 바꿔도 동작)
        @Size(max = 10)
        List<String> imageUrls,

        // 새 정석 방식
        @Size(max = 10)
        List<UUID> imageFileIds,

        @NotNull PostVisibility visibility
) {}