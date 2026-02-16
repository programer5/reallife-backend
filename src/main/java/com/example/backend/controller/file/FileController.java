package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.FileService;
import com.example.backend.service.file.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UploadedFileRepository uploadedFileRepository;
    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return fileService.upload(meId, file);
    }

    /**
     * ✅ 이미지/파일을 브라우저에서 바로 볼 수 있도록 inline 서빙
     * - 프론트 연결 시 <img src="...">가 바로 동작
     * - "무조건 다운로드"를 원하면 inline() -> attachment()로 변경하면 됨
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID fileId) {

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getOriginalFilename()).build().toString())
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }
}