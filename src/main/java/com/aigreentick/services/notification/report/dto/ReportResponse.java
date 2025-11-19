package com.aigreentick.services.notification.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String granularity;
    private List<DailyVolumeReportDTO> series;
}
