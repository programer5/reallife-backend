package com.example.backend.controller.home;

import com.example.backend.controller.home.dto.HomeTodayWidgetResponse;
import com.example.backend.service.home.HomeTodayWidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeTodayWidgetController {

    private final HomeTodayWidgetService homeTodayWidgetService;

    @GetMapping("/today-widget")
    public HomeTodayWidgetResponse todayWidget(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return homeTodayWidgetService.getTodayWidget(meId);
    }
}
