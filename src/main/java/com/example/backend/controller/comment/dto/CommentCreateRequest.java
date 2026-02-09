package com.example.backend.controller.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "content must not be blank")
        @Size(max = 1000, message = "content must be 1000 characters or less")
        String content
) {}