package com.docflow.ai.common.enums;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, 200, "SUCCESS", "成功"),
    BAD_REQUEST(40000, 400, "BAD_REQUEST", "请求有误"),
    VALIDATION_ERROR(40001, 400, "VALIDATION_ERROR", "请求参数校验失败"),
    UNAUTHORIZED(40100, 401, "AUTH_UNAUTHORIZED", "未登录或登录已过期"),
    TOKEN_EXPIRED(40101, 401, "AUTH_TOKEN_EXPIRED", "登录凭证已过期"),
    TOKEN_INVALID(40102, 401, "AUTH_TOKEN_INVALID", "登录凭证无效"),
    ACCOUNT_DISABLED(40103, 401, "AUTH_ACCOUNT_DISABLED", "账号已被禁用"),
    LOGIN_FAILED(40105, 401, "AUTH_LOGIN_FAILED", "用户名或密码错误"),
    REFRESH_TOKEN_INVALID(40106, 401, "AUTH_REFRESH_TOKEN_INVALID", "刷新凭证无效"),
    FORBIDDEN(40300, 403, "AUTH_FORBIDDEN", "当前账号没有访问权限"),
    NOT_FOUND(40400, 404, "RESOURCE_NOT_FOUND", "请求的资源不存在"),
    TICKET_NOT_FOUND(40401, 404, "TICKET_NOT_FOUND", "工单不存在"),
    RESOURCE_CONFLICT(40900, 409, "RESOURCE_CONFLICT", "当前资源状态冲突"),
    BUSINESS_RULE_VIOLATION(42200, 422, "BUSINESS_RULE_VIOLATION", "当前操作不符合业务规则"),
    TICKET_ALREADY_CLOSED(42202, 422, "TICKET_ALREADY_CLOSED", "工单已关闭，无法操作"),
    RATE_LIMITED(42900, 429, "RATE_LIMITED", "请求过于频繁，请稍后重试"),
    ERROR(50000, 500, "INTERNAL_SERVER_ERROR", "系统开小差了"),
    DATABASE_ERROR(50001, 500, "DATABASE_ERROR", "数据库操作失败"),
    EXTERNAL_SERVICE_ERROR(50002, 500, "EXTERNAL_SERVICE_ERROR", "外部服务调用失败"),
    AI_SERVICE_ERROR(50005, 500, "AI_SERVICE_ERROR", "AI 服务调用失败"),
    CIRCUIT_BREAKER_OPEN(50300, 503, "CIRCUIT_BREAKER_OPEN", "服务暂时不可用"),
    EXTERNAL_SERVICE_TIMEOUT(50400, 504, "EXTERNAL_SERVICE_TIMEOUT", "外部服务调用超时");

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
