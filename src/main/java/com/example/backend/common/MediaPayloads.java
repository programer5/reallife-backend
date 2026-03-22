package com.example.backend.common;

import java.util.Locale;
import java.util.UUID;

public final class MediaPayloads {

    private MediaPayloads() {}

    public static String normalizeMediaType(String contentType, String fallback) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String fb = fallback == null ? "" : fallback.trim().toUpperCase(Locale.ROOT);
        if (ct.startsWith("image/")) return "IMAGE";
        if (ct.startsWith("video/")) return "VIDEO";
        if ("IMAGE".equals(fb) || "VIDEO".equals(fb) || "FILE".equals(fb)) return fb;
        return "FILE";
    }

    public static String thumbnailUrl(String mediaType, UUID fileId, String fallbackUrl) {
        if (fileId == null) return fallbackUrl;
        if ("IMAGE".equalsIgnoreCase(mediaType) || "VIDEO".equalsIgnoreCase(mediaType)) {
            return "/api/files/" + fileId + "/thumbnail";
        }
        return fallbackUrl;
    }

    public static String previewUrl(String mediaType, String downloadUrl) {
        return downloadUrl;
    }

    public static String streamingUrl(String mediaType, String downloadUrl) {
        if ("VIDEO".equalsIgnoreCase(mediaType)) return downloadUrl;
        return null;
    }
}
