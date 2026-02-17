package com.example.backend.service.file;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    String store(MultipartFile file);      // ✅ meId 제거
    Path resolvePath(String fileKey);
    void delete(String fileKey);
}