package com.example.backend.service.file;

import com.example.backend.common.PublicUrlBuilder;
import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.file.event.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ 추가
    private final PublicUrlBuilder urlBuilder;

    @Transactional
    public FileUploadResponse upload(UUID meId, MultipartFile file) {
        userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

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

        String ct = (saved.getContentType() == null) ? "" : saved.getContentType().toLowerCase(Locale.ROOT);

        // ✅ 절대 URL로 만들어서 내려주기
        String downloadUrl = urlBuilder.absolute("/api/files/" + saved.getId() + "/download");

        String thumbnailUrl = null;
        if (ct.startsWith("image/")) {
            eventPublisher.publishEvent(new FileUploadedEvent(saved.getId()));
            thumbnailUrl = urlBuilder.absolute("/api/files/" + saved.getId() + "/thumbnail");
        }

        return new FileUploadResponse(
                saved.getId(),
                downloadUrl,
                thumbnailUrl,
                saved.getOriginalFilename(),
                saved.getContentType(),
                saved.getSize()
        );
    }
}