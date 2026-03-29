package com.example.backend.controller.playback;

import com.example.backend.controller.playback.dto.PlaybackSessionCreateRequest;
import com.example.backend.controller.playback.dto.PlaybackSessionListResponse;
import com.example.backend.controller.playback.dto.PlaybackSessionResponse;
import com.example.backend.controller.playback.dto.PlaybackSessionStateUpdateRequest;
import com.example.backend.service.playback.PlaybackSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/playback-sessions")
public class PlaybackSessionController {

    private final PlaybackSessionService playbackSessionService;

    @GetMapping
    public PlaybackSessionListResponse list(@PathVariable UUID conversationId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return playbackSessionService.list(conversationId, meId);
    }

    @PostMapping
    public PlaybackSessionResponse create(
            @PathVariable UUID conversationId,
            @Valid @RequestBody PlaybackSessionCreateRequest request,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return playbackSessionService.create(conversationId, meId, request);
    }

    @PatchMapping("/{sessionId}/state")
    public PlaybackSessionResponse updateState(
            @PathVariable UUID conversationId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody PlaybackSessionStateUpdateRequest request,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return playbackSessionService.updateState(conversationId, sessionId, meId, request);
    }

    @PostMapping("/{sessionId}/presence")
    public PlaybackSessionResponse touchPresence(
            @PathVariable UUID conversationId,
            @PathVariable UUID sessionId,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        return playbackSessionService.touchPresence(conversationId, sessionId, meId);
    }

    @PostMapping("/{sessionId}/end")
    public PlaybackSessionResponse end(
            @PathVariable UUID conversationId,
            @PathVariable UUID sessionId,
            @RequestBody(required = false) PlaybackSessionStateUpdateRequest request,
            Authentication authentication
    ) {
        UUID meId = UUID.fromString(authentication.getName());
        long positionSeconds = request == null ? 0L : request.positionSeconds();
        return playbackSessionService.end(conversationId, sessionId, meId, positionSeconds);
    }
}
