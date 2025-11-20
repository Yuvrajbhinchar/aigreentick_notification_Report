package com.aigreentick.services.notification.report.controller;

import com.aigreentick.services.notification.report.dto.SummaryReportDTO;
import com.aigreentick.services.notification.report.service.NotificationSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class NotificationSummaryController {

    private final NotificationSummaryService notificationSummaryService;


    /**
     * GET /api/reports/summary?start=2025-01-15T00:00:00&end=2025-01-17T23:59:59
     */
    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam("start") String startStr,
                                     @RequestParam("end")   String endStr) {
        try {
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end   = LocalDateTime.parse(endStr);

            if (end.isBefore(start)) {
                return ResponseEntity.badRequest().body("end must be after or equal to start");
            }

            // Safety limit: restrict huge ranges
            long days = java.time.Duration.between(start, end).toDays() + 1;
            if (days > 365) {
                return ResponseEntity.badRequest().body("Range too large. Use up to 365 days or implement rollups.");
            }

            SummaryReportDTO dto = notificationSummaryService.getSummary(start, end);
            return ResponseEntity.ok(dto);

        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("Invalid datetime format. Use ISO: yyyy-MM-ddTHH:mm:ss");
        } catch (Exception ex) {
            // log
            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
