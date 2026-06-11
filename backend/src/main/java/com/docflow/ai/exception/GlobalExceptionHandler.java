package com.docflow.ai.exception;

import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.common.domain.ApiValidationError;
import com.docflow.ai.common.enums.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.docflow.ai.common.resilience.ResilienceService;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        // BusinessException 表示“预期内失败”，例如权限不足、状态冲突、资源不存在。
        log.warn("business exception: method={}, uri={}, code={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getCode(),
                ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(buildErrorResponse(ex.getCode(), ex.getError(), ex.getMessage(), request.getRequestURI(), null));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception ex, HttpServletRequest request) {
        // 把 Spring 各种参数校验异常收敛成统一的 errors 列表，方便前端直接展示。
        List<ApiValidationError> validationErrors = resolveValidationErrors(ex);
        String message = validationErrors.isEmpty()
                ? ResultCode.BAD_REQUEST.getMessage()
                : validationErrors.stream().map(ApiValidationError::getMessage).collect(Collectors.joining("; "));
        log.warn("bad request: method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                message);
        return ResponseEntity.badRequest().body(buildErrorResponse(
                ResultCode.VALIDATION_ERROR.getCode(),
                ResultCode.VALIDATION_ERROR.getError(),
                message,
                request.getRequestURI(),
                validationErrors
        ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NoSuchElementException ex,
                                                                     HttpServletRequest request) {
        log.warn("entity not found: method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ResultCode.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex,
                                                                         HttpServletRequest request) {
        log.warn("access denied: method={}, uri={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ResultCode.FORBIDDEN, request.getRequestURI()));
    }


    // ==================== Circuit Breaker ====================

    @ExceptionHandler(ResilienceService.CircuitBreakerOpenException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(ResilienceService.CircuitBreakerOpenException ex, HttpServletRequest req) {
        log.warn("Circuit breaker OPEN: {} {} instance={}", req.getMethod(), req.getRequestURI(), ex.getInstanceName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<Void>builder()
                        .code(ResultCode.CIRCUIT_BREAKER_OPEN.getCode())
                        .error(ResultCode.CIRCUIT_BREAKER_OPEN.getError())
                        .message(ResultCode.CIRCUIT_BREAKER_OPEN.getMessage())
                        .path(req.getRequestURI())
                        .timestamp(java.time.OffsetDateTime.now())
                        .traceId(org.slf4j.MDC.get("traceId"))
                        .build());
    }

    @ExceptionHandler(ResilienceService.ResilienceExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleResilienceFailure(ResilienceService.ResilienceExecutionException ex, HttpServletRequest req) {
        log.error("Resilience failed: {} {} instance={}", req.getMethod(), req.getRequestURI(), ex.getInstanceName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .code(ResultCode.EXTERNAL_SERVICE_ERROR.getCode())
                        .error(ResultCode.EXTERNAL_SERVICE_ERROR.getError())
                        .message("Service [" + ex.getInstanceName() + "] temporarily unavailable")
                        .path(req.getRequestURI())
                        .timestamp(java.time.OffsetDateTime.now())
                        .traceId(org.slf4j.MDC.get("traceId"))
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        // 兜底异常对前端只返回统一文案，具体堆栈细节保留在日志里。
        log.error("unhandled exception: method={}, uri={}",
                request.getMethod(),
                request.getRequestURI(),
                ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorResponse(
                ResultCode.ERROR.getCode(),
                ResultCode.ERROR.getError(),
                ResultCode.ERROR.getMessage(),
                request.getRequestURI(),
                null
        ));
    }

    private List<ApiValidationError> resolveValidationErrors(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException methodArgumentNotValidException = (MethodArgumentNotValidException) ex;
            return methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(error -> ApiValidationError.builder()
                            .field(error.getField())
                            .message(error.getDefaultMessage())
                            .build())
                    .collect(Collectors.toList());
        }
        if (ex instanceof BindException) {
            BindException bindException = (BindException) ex;
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> ApiValidationError.builder()
                            .field(error.getField())
                            .message(error.getDefaultMessage())
                            .build())
                    .collect(Collectors.toList());
        }
        if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException constraintViolationException = (ConstraintViolationException) ex;
            return constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> ApiValidationError.builder()
                            .field(violation.getPropertyPath().toString())
                            .message(violation.getMessage())
                            .build())
                    .collect(Collectors.toList());
        }
        return new ArrayList<ApiValidationError>();
    }

    private ApiResponse<Void> buildErrorResponse(Integer code,
                                                 String error,
                                                 String message,
                                                 String path,
                                                 List<ApiValidationError> validationErrors) {
        // traceId 来自 RequestTraceFilter 放进 MDC 的值，用于把错误响应和日志串起来。
        return ApiResponse.<Void>builder()
                .code(code)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(java.time.OffsetDateTime.now())
                .errors(validationErrors)
                .traceId(org.slf4j.MDC.get("traceId"))
                .build();
    }
}
