package com.example.backend.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OpsAlertHistoryResponse {

    private List<OpsAlertHistoryItem> items;
}