package com.example.backend.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String reason) {}
}
