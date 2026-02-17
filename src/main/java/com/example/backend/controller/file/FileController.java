package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.StorageService;
import com.example.backend.service.file.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.DigestUtils;
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

    // ================= 업로드 =================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return fileService.upload(meId, file);
    }

    // ================= 원본 다운로드 =================
    @GetMapping("/{fileId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID fileId,
                                                       HttpServletRequest request) {

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        String etag = generateETag(file.getId(), file.getFileKey(), file.getSize());
        if (etagMatch(request, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        FileSystemResource resource = new FileSystemResource(path);

        String ct = (file.getContentType() == null) ? "application/octet-stream" : file.getContentType();
        boolean isImage = ct.toLowerCase(Locale.ROOT).startsWith("image/");

        ContentDisposition disposition = isImage
                ? ContentDisposition.inline().filename(file.getOriginalFilename()).build()
                : ContentDisposition.attachment().filename(file.getOriginalFilename()).build();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable())
                .eTag(etag)
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    // ================= 썸네일 =================
    @GetMapping("/{fileId}/thumbnail")
    public ResponseEntity<FileSystemResource> thumbnail(@PathVariable UUID fileId,
                                                        HttpServletRequest request) {

        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        if (!file.hasThumbnail()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        Path path = storageService.resolvePath(file.getThumbnailFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        String etag = generateETag(file.getId(), file.getThumbnailFileKey(), file.getThumbnailSize());
        if (etagMatch(request, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        FileSystemResource resource = new FileSystemResource(path);

        String ct = (file.getThumbnailContentType() == null) ? "image/jpeg" : file.getThumbnailContentType();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable())
                .eTag(etag)
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("thumbnail-" + file.getOriginalFilename()).build().toString())
                .body(resource);
    }

    // ================= ETag 유틸 =================
    private String generateETag(UUID id, String key, Long size) {
        String raw = id + ":" + key + ":" + size;
        return "\"" + DigestUtils.md5DigestAsHex(raw.getBytes()) + "\"";
    }

    private boolean etagMatch(HttpServletRequest request, String etag) {
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        return etag.equals(ifNoneMatch);
    }
}