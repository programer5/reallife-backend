package com.example.backend.scheduler;

import com.example.backend.service.file.FileGarbageCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileGarbageCollectorScheduler {

    private final FileGarbageCollectorService service;

    // 매일 새벽 3시
    @Scheduled(cron = "0 0 3 * * *")
//    @Scheduled(fixedDelay = 60000)
    public void run() {
        log.info("[FileGC] 스케줄러 실행");
        service.cleanOrphanFiles();
    }
}