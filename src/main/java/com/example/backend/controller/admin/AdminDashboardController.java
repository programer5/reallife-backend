package com.example.backend.controller.admin;

import com.example.backend.monitoring.dto.AdminDashboardResponse;
import com.example.backend.monitoring.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return adminDashboardService.getDashboard();
    }
}