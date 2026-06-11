package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.TicketReportResponse;

public interface ReportService {
    TicketReportResponse generateDailyReport();
    TicketReportResponse generateWeeklyReport();
}
