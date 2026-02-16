package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.FileService;
import com.example.backend.service.file.LocalStorageService;
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
    private final LocalStorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return fileService.upload(meId, file);
    }

    // ✅ 파일 다운로드/서빙 API 추가
    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileSystemResource> download(
            @PathVariable UUID fileId,
            Authentication authentication
    ) {
        // 인증은 SecurityConfig에서 기본 보호(.anyRequest().authenticated())
        UUID meId = UUID.fromString(authentication.getName());

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // (선택) 업로더만 다운로드 가능하게 하고 싶으면 아래 체크 추가 가능
        // if (!file.getUploaderId().equals(meId)) throw new BusinessException(ErrorCode.FILE_FORBIDDEN);

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.getOriginalFilename()).build().toString())
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }
}