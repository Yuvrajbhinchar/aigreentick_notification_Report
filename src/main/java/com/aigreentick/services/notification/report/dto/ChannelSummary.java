package com.aigreentick.services.notification.report.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChannelSummary {
    private long total = 0L;
    private Map<String, Long> statusCounts = new HashMap<>();
}
