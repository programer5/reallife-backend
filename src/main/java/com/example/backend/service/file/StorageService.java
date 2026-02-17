package com.example.backend.service.file;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {

    String store(MultipartFile file);

    // ✅ 썸네일/바이너리 저장용
    String storeBytes(byte[] bytes, String extension);

    Path resolvePath(String fileKey);

    // ✅ GC/썸네일 삭제용
    void delete(String fileKey);
}