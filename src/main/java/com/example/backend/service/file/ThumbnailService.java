
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
import java.nio.file.Files;
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
        if (file == null || file.hasThumbnail()) return;

        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        try {
            Path originalPath = storageService.resolvePath(file.getFileKey());
            if (!originalPath.toFile().exists()) return;

            if (ct.startsWith("image/")) {
                generateImageThumbnail(file, originalPath);
                return;
            }
            if (ct.startsWith("video/")) {
                generateVideoThumbnail(file, originalPath);
            }
        } catch (Exception e) {
            log.error("[Thumbnail] generate failed fileId={}", fileId, e);
        }
    }

    private void generateImageThumbnail(UploadedFile file, Path originalPath) throws Exception {
        String outFormat = (file.getContentType().equalsIgnoreCase("image/png")) ? "png" : "jpg";
        String outContentType = outFormat.equals("png") ? "image/png" : "image/jpeg";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(originalPath.toFile())
                .size(400, 400)
                .outputFormat(outFormat)
                .toOutputStream(baos);

        byte[] bytes = baos.toByteArray();
        String thumbKey = storageService.storeBytes(bytes, "." + outFormat);
        file.attachThumbnail(thumbKey, outContentType, bytes.length);
    }

    private void generateVideoThumbnail(UploadedFile file, Path originalPath) {
        try {
            Path tempOut = storageService.resolvePath(UUID.randomUUID() + ".jpg");
            Process process = new ProcessBuilder(
                    "ffmpeg", "-y", "-ss", "00:00:01", "-i", originalPath.toString(),
                    "-frames:v", "1", "-vf", "scale='min(640,iw)':-2", tempOut.toString()
            ).redirectErrorStream(true).start();

            int exit = process.waitFor();
            if (exit != 0 || !tempOut.toFile().exists()) {
                log.warn("[Thumbnail] ffmpeg unavailable or failed for fileId={}", file.getId());
                return;
            }

            byte[] bytes = Files.readAllBytes(tempOut);
            String thumbKey = storageService.storeBytes(bytes, ".jpg");
            file.attachThumbnail(thumbKey, "image/jpeg", bytes.length);
            Files.deleteIfExists(tempOut);
        } catch (Exception e) {
            log.warn("[Thumbnail] video thumbnail skipped fileId={} - {}", file.getId(), e.getMessage());
        }
    }
}
