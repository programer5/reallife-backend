package com.example.backend.controller.file.dto;

import java.util.UUID;

public record FileUploadResponse(
        UUID fileId,
        String mediaType,
        String url,
        String downloadUrl,
        String previewUrl,
        String thumbnailUrl,
        String streamingUrl,
        String originalFilename,
        String contentType,
        long size,
        String fileType
) {}
