package com.example.backend.service.file;

import com.example.backend.domain.file.UploadedFile;
import com.example.backend.repository.file.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileGarbageCollectorService {

    private final UploadedFileRepository uploadedFileRepository;
    private final StorageService storageService;

    @Transactional
    public void cleanOrphanFiles() {

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        List<UploadedFile> targets = uploadedFileRepository.findOrphanFiles(threshold);

        if (targets.isEmpty()) {
            log.info("[FileGC] 삭제할 고아 파일 없음");
            return;
        }

        log.info("[FileGC] 고아 파일 {}개 삭제 시작", targets.size());

        for (UploadedFile file : targets) {
            try {
                // 실제 파일 삭제
                storageService.delete(file.getFileKey());

                // soft delete
                file.delete();

                log.info("[FileGC] 삭제 완료: {}", file.getId());

            } catch (Exception e) {
                log.error("[FileGC] 삭제 실패: {}", file.getFileKey(), e);
            }
        }
    }
}