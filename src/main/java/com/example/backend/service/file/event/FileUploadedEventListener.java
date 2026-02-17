package com.example.backend.service.file.event;

import com.example.backend.service.file.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadedEventListener {

    private final ThumbnailService thumbnailService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(FileUploadedEvent event) {
        log.info("[FileEvent] AFTER_COMMIT fileId={}", event.fileId());
        thumbnailService.generateThumbnail(event.fileId());
    }
}