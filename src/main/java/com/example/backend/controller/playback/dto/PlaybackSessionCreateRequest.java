package com.example.backend.controller.playback.dto;

import com.example.backend.domain.playback.PlaybackMediaKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaybackSessionCreateRequest(
        @NotNull PlaybackMediaKind mediaKind,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 1000) String sourceUrl,
        @Size(max = 1000) String thumbnailUrl
) {
}
