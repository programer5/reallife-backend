package com.example.backend.controller.admin;

import com.example.backend.domain.error.ErrorLog;
import com.example.backend.service.error.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminErrorController {

    private final ErrorLogService errorLogService;

    @GetMapping("/admin/errors")
    public List<ErrorLog> getRecentErrors() {
        return errorLogService.getRecentErrors();
    }
}