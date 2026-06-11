package com.docflow.ai.common.chain;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 责任链处理上下文。
 * 携带工单数据和处理过程中的状态信息。
 */
@Data
@Builder
public class ProcessingContext {
    private Ticket ticket;
    private Long operatorId;
    private String action;          // CREATE, UPDATE, ASSIGN, CLOSE
    private boolean shouldContinue;
    @Builder.Default
    private List<String> processingLog = new ArrayList<>();

    public void addLog(String log) {
        processingLog.add(log);
    }
}
