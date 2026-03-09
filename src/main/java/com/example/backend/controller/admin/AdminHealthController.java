package com.example.backend.controller.admin;

import com.example.backend.monitoring.dto.AdminHealthResponse;
import com.example.backend.monitoring.dto.RealtimeHealthResponse;
import com.example.backend.monitoring.dto.ReminderHealthResponse;
import com.example.backend.monitoring.service.AdminHealthService;
import com.example.backend.monitoring.service.RealtimeHealthService;
import com.example.backend.monitoring.service.ReminderHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/health")
public class AdminHealthController {

    private final AdminHealthService adminHealthService;
    private final RealtimeHealthService realtimeHealthService;
    private final ReminderHealthService reminderHealthService;

    @GetMapping
    public AdminHealthResponse health() {
        return adminHealthService.getAdminHealth();
    }

    @GetMapping("/realtime")
    public RealtimeHealthResponse realtime() {
        return realtimeHealthService.getRealtimeHealth();
    }

    @GetMapping("/reminder")
    public ReminderHealthResponse reminder() {
        return reminderHealthService.getReminderHealth();
    }
}