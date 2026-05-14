package com.docflow.ai.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, 200, "SUCCESS", "成功"),

    BAD_REQUEST(40000, 400, "BAD_REQUEST", "请求有误，请检查后重试"),
    VALIDATION_ERROR(40001, 400, "VALIDATION_ERROR", "请求参数校验失败"),

    UNAUTHORIZED(40100, 401, "AUTH_UNAUTHORIZED", "未登录或登录已过期"),
    FORBIDDEN(40300, 403, "AUTH_FORBIDDEN", "当前账号没有访问权限"),
    NOT_FOUND(40400, 404, "RESOURCE_NOT_FOUND", "请求的资源不存在"),

    BUSINESS_RULE_VIOLATION(42200, 422, "BUSINESS_RULE_VIOLATION", "当前操作不符合业务规则"),
    RESOURCE_CONFLICT(40900, 409, "RESOURCE_CONFLICT", "当前资源状态冲突，请刷新后重试"),

    ERROR(50000, 500, "INTERNAL_SERVER_ERROR", "系统开小差了，请稍后重试");

    private final int code;
    private final int httpStatus;
    private final String error;
    private final String message;

    ResultCode(int code, int httpStatus, String error, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.error = error;
        this.message = message;
    }
}
