package com.example.backend.controller.file.dto;

import java.util.UUID;

public record FileUploadResponse(
        UUID fileId,
        String url,
        String originalFilename,
        String contentType,
        long size
) {}