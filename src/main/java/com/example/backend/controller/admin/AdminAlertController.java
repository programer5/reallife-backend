package com.example.backend.controller.admin;

import com.example.backend.ops.OpsAlertService;
import com.example.backend.ops.dto.AdminAlertTestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/alerts")
public class AdminAlertController {

    private final OpsAlertService opsAlertService;

    @PostMapping("/test")
    public AdminAlertTestResponse sendSlackTestAlert(Authentication authentication) {
        String requestedBy = authentication != null ? authentication.getName() : "anonymous";
        return opsAlertService.sendSlackTestAlert(requestedBy);
    }
}