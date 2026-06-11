package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(Long userId);
}
