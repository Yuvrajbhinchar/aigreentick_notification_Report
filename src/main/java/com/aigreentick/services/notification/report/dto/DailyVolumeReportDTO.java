package com.aigreentick.services.notification.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DailyVolumeReportDTO {
    private String date;
    private ChannelSummary email = new ChannelSummary();
    private ChannelSummary push = new ChannelSummary();
    private long total;
}

