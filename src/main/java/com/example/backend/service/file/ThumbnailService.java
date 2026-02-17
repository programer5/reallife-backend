package com.example.backend.service.file;

import com.example.backend.domain.file.UploadedFile;
import com.example.backend.repository.file.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final UploadedFileRepository uploadedFileRepository;
    private final StorageService storageService;

    @Async
    @Transactional
    public void generateThumbnail(UUID fileId) {
        UploadedFile file = uploadedFileRepository.findByIdAndDeletedFalse(fileId).orElse(null);
        if (file == null) return;

        // ✅ 이미지가 아니면 스킵
        if (file.getContentType() == null || !file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            return;
        }

        // 이미 썸네일 있으면 스킵
        if (file.hasThumbnail()) return;

        try {
            Path originalPath = storageService.resolvePath(file.getFileKey());
            if (!originalPath.toFile().exists()) return;

            String outFormat = (file.getContentType().equalsIgnoreCase("image/png")) ? "png" : "jpg";
            String outContentType = outFormat.equals("png") ? "image/png" : "image/jpeg";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // ✅ 400x400 안에 맞추면서 비율 유지
            Thumbnails.of(originalPath.toFile())
                    .size(400, 400)
                    .outputFormat(outFormat)
                    .toOutputStream(baos);

            byte[] bytes = baos.toByteArray();
            String thumbKey = storageService.storeBytes(bytes, "." + outFormat);

            file.attachThumbnail(thumbKey, outContentType, bytes.length);

            log.info("[Thumbnail] generated fileId={}, thumbKey={}", fileId, thumbKey);

        } catch (Exception e) {
            log.error("[Thumbnail] generate failed fileId={}", fileId, e);
        }
    }
}