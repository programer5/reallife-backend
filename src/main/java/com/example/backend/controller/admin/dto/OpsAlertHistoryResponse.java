package com.example.backend.controller.admin.dto;

import java.util.List;

public record OpsAlertHistoryResponse(
        List<OpsAlertHistoryItemResponse> items
) {
}