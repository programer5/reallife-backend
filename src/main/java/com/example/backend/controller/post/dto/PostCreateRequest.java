package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PostCreateRequest(
        @NotBlank String content,
        List<String> imageUrls,
        @NotNull PostVisibility visibility
) {
}
