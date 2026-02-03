package com.example.backend.service.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final UploadedFileRepository uploadedFileRepository;
    private final LocalFileStorage localFileStorage;

    @Transactional
    public FileUploadResponse upload(UUID uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String contentType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();
        long size = file.getSize();

        // 저장용 파일명: UUID + 확장자(있으면)
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > -1 && dot < originalName.length() - 1) ext = originalName.substring(dot);
        String storedFilename = UUID.randomUUID() + ext;

        try {
            localFileStorage.store(storedFilename, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        UploadedFile saved = uploadedFileRepository.save(
                UploadedFile.create(uploaderId, originalName, storedFilename, contentType, size)
        );

        return new FileUploadResponse(
                saved.getId(),
                "/api/files/" + saved.getId(),
                saved.getOriginalFilename(),
                saved.getContentType(),
                saved.getSize()
        );
    }

    @Transactional(readOnly = true)
    public UploadedFile getFile(UUID fileId) {
        return uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
    }
}