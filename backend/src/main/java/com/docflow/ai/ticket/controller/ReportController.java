package com.docflow.ai.ticket.controller;

import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.ticket.dto.TicketReportResponse;
import com.docflow.ai.ticket.service.ReportService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Daily and weekly ticket reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    @Timed(value = "reports.read", histogram = true)
    @Operation(summary = "Generate daily ticket report")
    public ApiResponse<TicketReportResponse> dailyReport() {
        return ApiResponse.success(reportService.generateDailyReport());
    }

    @GetMapping("/weekly")
    @Timed(value = "reports.read", histogram = true)
    @Operation(summary = "Generate weekly ticket report")
    public ApiResponse<TicketReportResponse> weeklyReport() {
        return ApiResponse.success(reportService.generateWeeklyReport());
    }
}
