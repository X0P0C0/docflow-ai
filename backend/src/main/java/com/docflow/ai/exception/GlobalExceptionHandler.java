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
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
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
