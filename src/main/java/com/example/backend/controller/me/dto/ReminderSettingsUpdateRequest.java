package com.example.backend.controller.me.dto;

public record ReminderSettingsUpdateRequest(
        Boolean pinRemindBrowserNotify,
        Boolean pinRemindSound,
        Boolean pinRemindVibrate
) {}
