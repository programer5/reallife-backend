package com.example.backend.service.file.event;

import java.util.UUID;

public record FileUploadedEvent(UUID fileId) {
}