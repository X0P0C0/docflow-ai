package com.docflow.ai.ticket.statemachine;

import lombok.Data;

/**
 * 工单状态机上下文：携带状态转换所需的所有信息。
 */
@Data
public class TicketStateContext {
    private Long ticketId;
    private Long operatorId;
    private Integer fromStatus;
    private Integer toStatus;
    private String remark;
}
