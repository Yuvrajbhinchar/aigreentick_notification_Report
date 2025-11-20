package com.aigreentick.services.notification.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStats {
    private long sent;   // number of attempts (or accepted)
    private long failed; // number of failures
}
