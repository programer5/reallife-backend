
package com.example.backend.controller.me.dto;

public record ReminderSettingsResponse(
        boolean pinRemindBrowserNotify,
        boolean pinRemindSound,
        boolean pinRemindVibrate
) {}
