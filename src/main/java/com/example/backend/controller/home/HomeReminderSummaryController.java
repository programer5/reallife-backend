
package com.example.backend.controller.home;

import com.example.backend.controller.home.dto.HomeReminderSummaryResponse;
import com.example.backend.service.home.HomeReminderSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeReminderSummaryController {

    private final HomeReminderSummaryService homeReminderSummaryService;

    @GetMapping("/reminder-summary")
    public HomeReminderSummaryResponse reminderSummary(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        return homeReminderSummaryService.getSummary(meId);
    }
}
