package com.example.backend.controller.playback.dto;

import java.util.List;

public record PlaybackSessionListResponse(
        List<PlaybackSessionResponse> items
) {
}
