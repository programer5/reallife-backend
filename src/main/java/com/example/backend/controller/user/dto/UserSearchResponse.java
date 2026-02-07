package com.example.backend.controller.user.dto;

import java.util.List;
import java.util.UUID;

public record UserSearchResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext
) {
    public record Item(
            UUID userId,
            String handle,
            String name,
            long followerCount,
            int rank
    ) {}

    public record Cursor(int rank, long followerCount, String handle, UUID userId) {

        public static Cursor decode(String raw) {
            if (raw == null || raw.isBlank()) return null;
            try {
                String[] parts = raw.split("\\|");
                if (parts.length != 4) return null;
                return new Cursor(
                        Integer.parseInt(parts[0]),
                        Long.parseLong(parts[1]),
                        parts[2],
                        UUID.fromString(parts[3])
                );
            } catch (Exception e) {
                return null;
            }
        }

        public static String encode(int rank, long followerCount, String handle, UUID userId) {
            return rank + "|" + followerCount + "|" + handle + "|" + userId;
        }
    }
}