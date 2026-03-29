package com.example.backend.controller.playback.dto;

import com.example.backend.domain.playback.PlaybackState;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlaybackSessionStateUpdateRequest(
        @NotNull PlaybackState playbackState,
        @Min(0) long positionSeconds
) {
}
