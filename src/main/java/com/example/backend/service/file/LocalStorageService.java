package com.example.backend.service.file;

import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILES_PER_MESSAGE = 5;

    private static final Set<String> ALLOWED = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final Path root = Paths.get("uploads");

    public LocalStorageService() {
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int maxFilesPerMessage() {
        return MAX_FILES_PER_MESSAGE;
    }

    public StoredFile store(UUID conversationId, MultipartFile file) {
        validate(file);

        String ext = safeExt(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

        // key는 “스토리지 독립적인 키”
        String fileKey = "conv/" + conversationId + "/" + fileName;
        Path target = root.resolve(fileKey);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(
                    fileKey,
                    safeOriginalName(file.getOriginalFilename()),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public Path resolvePath(String fileKey) {
        return root.resolve(fileKey);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        if (file.getSize() > MAX_BYTES) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);

        String type = file.getContentType();
        if (!StringUtils.hasText(type) || !ALLOWED.contains(type)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String safeOriginalName(String name) {
        if (!StringUtils.hasText(name)) return "file";
        return name.replaceAll("[\\\\/\\r\\n\\t\\0]", "_");
    }

    private String safeExt(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        String ext = filename.substring(idx + 1).toLowerCase();
        return ext.replaceAll("[^a-z0-9]", "");
    }

    public record StoredFile(
            String fileKey,
            String originalName,
            String mimeType,
            long sizeBytes
    ) {}
}