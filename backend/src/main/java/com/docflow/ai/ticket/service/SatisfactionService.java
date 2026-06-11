package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.SatisfactionStatsResponse;

public interface SatisfactionService {
    SatisfactionStatsResponse getStats(Long userId);
}
