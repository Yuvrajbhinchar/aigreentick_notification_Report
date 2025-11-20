package com.aigreentick.services.notification.report.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryReportDTO {
    private String start; // ISO date-time
    private String end;   // ISO date-time

    private ChannelSummary email;
    private ChannelSummary push;

    private long overallTotal;
    private double overallSuccessRate; // 0-100 as percentage or decimal? here decimal (0-100)

    // provider -> stats
    private Map<String, ProviderStats> topProviders;
}
