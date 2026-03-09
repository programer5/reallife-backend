package com.example.backend.service.error;

import com.example.backend.domain.error.ErrorLog;
import com.example.backend.repository.error.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository repository;

    public void record(String type, String message, String path) {
        repository.save(ErrorLog.of(type, message, path));
    }

    public List<ErrorLog> getRecentErrors() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}