package com.example.backend.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path root;


    // ✅ application.yml의 file.upload-dir 사용 (ENV: FILE_UPLOAD_DIR)
    public LocalStorageService(@Value("${file.upload-dir:uploads}") String rootDir) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리 생성 실패: " + this.root, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = "";

        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        String fileKey = UUID.randomUUID() + ext;
        Path target = root.resolve(fileKey);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패", e);
        }

        return fileKey;
    }

    @Override
    public Path resolvePath(String fileKey) {
        return root.resolve(fileKey).normalize();
    }

    @Override
    public void delete(String fileKey) {
        try {
            Path filePath = resolvePath(fileKey);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.error("파일 삭제 실패: {}", fileKey, e);
        }
    }
}