package com.docflow.ai.exception;

import com.docflow.ai.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String error;
    private final Integer httpStatus;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_RULE_VIOLATION.getCode();
        this.error = ResultCode.BUSINESS_RULE_VIOLATION.getError();
        this.httpStatus = ResultCode.BUSINESS_RULE_VIOLATION.getHttpStatus();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.error = resultCode.getError();
        this.httpStatus = resultCode.getHttpStatus();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.error = ResultCode.BUSINESS_RULE_VIOLATION.getError();
        this.httpStatus = ResultCode.BUSINESS_RULE_VIOLATION.getHttpStatus();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.error = resultCode.getError();
        this.httpStatus = resultCode.getHttpStatus();
    }
}
