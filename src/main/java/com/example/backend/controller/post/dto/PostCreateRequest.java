package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank
        @Size(max = 2000, message = "content는 최대 2000자까지 가능합니다.")
        String content,

        @Size(max = 10, message = "이미지는 최대 10장까지 가능합니다.")
        List<@Size(max = 1000, message = "imageUrl 길이가 너무 깁니다.") String> imageUrls,

        @NotNull PostVisibility visibility
) {}
