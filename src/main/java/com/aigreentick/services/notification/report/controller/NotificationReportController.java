package com.aigreentick.services.notification.report.controller;

import java.time.LocalDate;

import com.aigreentick.services.notification.report.service.NotificationReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aigreentick.services.notification.report.dto.ReportResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class NotificationReportController {

    private final NotificationReportService service;

    @GetMapping("/daily-volume")
    public ResponseEntity<?> daily(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end
    ) {

        if (end.isBefore(start)) {
            return ResponseEntity.badRequest()
                    .body("endDate must not be before startDate");
        }

        var series = service.getDailyVolumeReport(start, end);

        return ResponseEntity.ok(new ReportResponse("daily", series));
    }
}

