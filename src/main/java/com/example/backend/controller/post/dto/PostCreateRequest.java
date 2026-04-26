
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

        @Size(max = 10)
        List<String> imageUrls,

        @Size(max = 10)
        List<UUID> imageFileIds,

        @Size(max = 10)
        List<UUID> mediaFileIds,


        Double latitude,
        Double longitude,

        @Size(max = 120)
        String placeName,

        @NotNull PostVisibility visibility
) {}
