package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchOperationResponse {
    private int successCount;
    private int failCount;
    private List<Long> failedIds;
    private List<String> errors;

    public static BatchOperationResponse of(int success, int fail, List<Long> failedIds, List<String> errors) {
        BatchOperationResponse r = new BatchOperationResponse();
        r.setSuccessCount(success);
        r.setFailCount(fail);
        r.setFailedIds(failedIds);
        r.setErrors(errors);
        return r;
    }
}
