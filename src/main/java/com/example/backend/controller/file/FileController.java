package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.StorageService;
import com.example.backend.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Locale;
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

    // ✅ 공개 다운로드 (브라우저 렌더링 위해 inline)
    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID fileId) {
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        FileSystemResource resource = new FileSystemResource(path);

        String ct = (file.getContentType() == null) ? "application/octet-stream" : file.getContentType();
        boolean isImage = ct.toLowerCase(Locale.ROOT).startsWith("image/");

        ContentDisposition disposition = isImage
                ? ContentDisposition.inline().filename(file.getOriginalFilename()).build()
                : ContentDisposition.attachment().filename(file.getOriginalFilename()).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    // ✅ 썸네일 다운로드 (없으면 404)
    @GetMapping("/{fileId}/thumbnail")
    public ResponseEntity<FileSystemResource> thumbnail(@PathVariable UUID fileId) {
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        if (!file.hasThumbnail()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        Path path = storageService.resolvePath(file.getThumbnailFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        FileSystemResource resource = new FileSystemResource(path);

        String ct = (file.getThumbnailContentType() == null) ? "image/jpeg" : file.getThumbnailContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("thumbnail-" + file.getOriginalFilename()).build().toString())
                .body(resource);
    }
}