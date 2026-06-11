package com.docflow.ai.common.pattern.template;

/**
 * 工单处理结果
 */
public record TicketProcessingResult(
    boolean success,
    String message,
    Long assignedUserId
) {
    public static TicketProcessingResult success(Long assignedUserId) {
        return new TicketProcessingResult(true, "处理成功", assignedUserId);
    }

    public static TicketProcessingResult failure(String message) {
        return new TicketProcessingResult(false, message, null);
    }
}
