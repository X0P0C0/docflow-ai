package com.docflow.ai.common.domain;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.logging.TraceIdConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String error;
    private String message;
    private T data;
    private String path;
    private OffsetDateTime timestamp;
    private List<ApiValidationError> errors;
    private String traceId;

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>baseBuilder()
                .code(ResultCode.SUCCESS.getCode())
                .error(ResultCode.SUCCESS.getError())
                .message(ResultCode.SUCCESS.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>baseBuilder()
                .code(ResultCode.SUCCESS.getCode())
                .error(ResultCode.SUCCESS.getError())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return ApiResponse.<T>baseBuilder()
                .code(resultCode.getCode())
                .error(resultCode.getError())
                .message(resultCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return ApiResponse.<T>baseBuilder()
                .code(code)
                .error(ResultCode.BAD_REQUEST.getError())
                .message(message)
                .build();
    }

    private static <T> ApiResponseBuilder<T> baseBuilder() {
        return ApiResponse.<T>builder()
                .timestamp(OffsetDateTime.now())
                .traceId(MDC.get(TraceIdConstants.TRACE_ID));
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode, String path) {
        return ApiResponse.<T>baseBuilder()
                .code(resultCode.getCode())
                .error(resultCode.getError())
                .message(resultCode.getMessage())
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message, String path) {
        return ApiResponse.<T>baseBuilder()
                .code(resultCode.getCode())
                .error(resultCode.getError())
                .message(message)
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message, String path, List<ApiValidationError> errors) {
        return ApiResponse.<T>baseBuilder()
                .code(resultCode.getCode())
                .error(resultCode.getError())
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }
}
