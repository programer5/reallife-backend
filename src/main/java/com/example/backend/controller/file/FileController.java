
package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.domain.file.UploadedFile;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.file.UploadedFileRepository;
import com.example.backend.service.file.FileService;
import com.example.backend.service.file.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/{fileId}/download")
    public ResponseEntity<?> download(@PathVariable UUID fileId, HttpServletRequest request) {
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        Path path = storageService.resolvePath(file.getFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        String etag = generateETag(file.getId(), file.getFileKey(), file.getSize());
        if (etagMatch(request, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        String ct = (file.getContentType() == null) ? "application/octet-stream" : file.getContentType();
        boolean isImage = ct.toLowerCase(Locale.ROOT).startsWith("image/");
        boolean isVideo = ct.toLowerCase(Locale.ROOT).startsWith("video/");

        ContentDisposition disposition = (isImage || isVideo)
                ? ContentDisposition.inline().filename(file.getOriginalFilename()).build()
                : ContentDisposition.attachment().filename(file.getOriginalFilename()).build();

        String range = request.getHeader(HttpHeaders.RANGE);
        if (isVideo && range != null && range.startsWith("bytes=")) {
            try {
                long fileLength = path.toFile().length();
                String[] ranges = range.replace("bytes=", "").split("-");
                long start = Long.parseLong(ranges[0]);
                long end = (ranges.length > 1 && !ranges[1].isBlank()) ? Long.parseLong(ranges[1]) : Math.min(start + 1024 * 1024 - 1, fileLength - 1);
                if (start > end || start >= fileLength) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                            .build();
                }
                end = Math.min(end, fileLength - 1);
                long contentLength = end - start + 1;

                byte[] data = new byte[(int) contentLength];
                try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                    raf.seek(start);
                    raf.readFully(data);
                }

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable())
                        .eTag(etag)
                        .contentType(MediaType.parseMediaType(ct))
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                        .contentLength(contentLength)
                        .body(data);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable())
                .eTag(etag)
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCEPT_RANGES, isVideo ? "bytes" : "none")
                .body(resource);
    }

    @GetMapping("/{fileId}/thumbnail")
    public ResponseEntity<FileSystemResource> thumbnail(@PathVariable UUID fileId, HttpServletRequest request) {
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        if (!file.hasThumbnail()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        Path path = storageService.resolvePath(file.getThumbnailFileKey());
        if (!path.toFile().exists()) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);

        String etag = generateETag(file.getId(), file.getThumbnailFileKey(), file.getThumbnailSize() == null ? 0L : file.getThumbnailSize());
        if (etagMatch(request, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        FileSystemResource resource = new FileSystemResource(path);
        String ct = (file.getThumbnailContentType() == null) ? "image/jpeg" : file.getThumbnailContentType();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable())
                .eTag(etag)
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("thumb-" + file.getOriginalFilename()).build().toString())
                .body(resource);
    }

    private String generateETag(UUID fileId, String key, long size) {
        String raw = fileId + ":" + key + ":" + size;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private boolean etagMatch(HttpServletRequest request, String etag) {
        String inm = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        return inm != null && inm.equals(etag);
    }
}
