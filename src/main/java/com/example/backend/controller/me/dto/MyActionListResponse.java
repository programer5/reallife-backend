
package com.example.backend.controller.me.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MyActionListResponse(
        List<Item> items
) {
    public record Item(
            UUID pinId,
            String type,
            String title,
            String placeText,
            LocalDateTime startAt,
            String status
    ) {}
}
