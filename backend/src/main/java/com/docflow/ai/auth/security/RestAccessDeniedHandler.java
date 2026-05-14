package com.docflow.ai.auth.security;

import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.logging.TraceIdConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(TraceIdConstants.TRACE_HEADER, MDC.get(TraceIdConstants.TRACE_ID));
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ResultCode.FORBIDDEN, request.getRequestURI())
        ));
    }
}
