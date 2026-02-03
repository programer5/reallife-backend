package com.example.backend.controller.file;

import com.example.backend.controller.file.dto.FileUploadResponse;
import com.example.backend.service.file.FileService;
import com.example.backend.service.file.LocalFileStorage;
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
@RequestMapping("/api")
public class FileController {

    private final FileService fileService;
    private final LocalFileStorage localFileStorage;

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return fileService.upload(meId, file);
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID fileId) {
        var meta = fileService.getFile(fileId);
        Path path = localFileStorage.load(meta.getStoredFilename());

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(meta.getOriginalFilename()).build().toString())
                .body(resource);
    }
}