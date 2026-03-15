package com.example.backend.controller.post.dto;

import com.example.backend.domain.post.PostVisibility;

public record PostUpdateRequest(
        String content,
        PostVisibility visibility
) {}
