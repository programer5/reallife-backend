package com.example.backend.controller.file.dto;

import java.util.UUID;

public record FileUploadResponse(
        UUID fileId,
        String url,
        String thumbnailUrl, // ✅ 추가 (없으면 null)
        String originalFilename,
        String contentType,
        long size
) {}