package com.example.backend.service.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService; // ✅ 인터페이스만 의존
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;

    @Transactional
    public FileUploadResponse upload(UUID meId, MultipartFile file) {
        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // ✅ localStorageService 아니라 storageService 사용
        String fileKey = storageService.store(file);

        UploadedFile saved = uploadedFileRepository.save(
                UploadedFile.create(
                        meId,
                        file.getOriginalFilename(),
                        fileKey,
                        file.getContentType(),
                        file.getSize()
                )
        );

        return new FileUploadResponse(
                saved.getId(),
                "/api/files/" + saved.getId() + "/download", // ✅ 다운로드 URL로 통일
                saved.getOriginalFilename(),
                saved.getContentType(),
                saved.getSize()
        );
    }
}