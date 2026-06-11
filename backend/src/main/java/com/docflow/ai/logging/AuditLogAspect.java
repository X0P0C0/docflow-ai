package com.docflow.ai.logging;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志 AOP 切面。
 * <p>
 * 拦截所有标注了 @AuditLog 的方法，自动记录：
 * <ul>
 *   <li>谁操作的（userId, username）</li>
 *   <li>做了什么（module, action）</li>
 *   <li>请求信息（method, uri, clientIp）</li>
 *   <li>执行结果（statusCode, success, durationMs, errorMessage）</li>
 * </ul>
 * <p>
 * 异步写入数据库（@Async），不影响接口响应速度。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLogEntity entity = new AuditLogEntity();
        entity.setModule(auditLog.module());
        entity.setAction(auditLog.action());
        entity.setOperateTime(LocalDateTime.now());
        entity.setTraceId(MDC.get(TraceIdConstants.TRACE_ID));

        // 填充用户信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUserPrincipal principal) {
            entity.setUserId(principal.getUserId());
            entity.setUsername(principal.getUsername());
        }

        // 填充请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            entity.setMethod(request.getMethod());
            entity.setUri(request.getRequestURI());
            entity.setClientIp(getClientIp(request));
        }

        try {
            Object result = joinPoint.proceed();
            entity.setSuccess(1);
            entity.setStatusCode(200);
            return result;
        } catch (Throwable ex) {
            entity.setSuccess(0);
            entity.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            entity.setDurationMs(System.currentTimeMillis() - startTime);
            // 异步写入，避免阻塞主线程
            try {
                auditLogMapper.insert(entity);
            } catch (Exception e) {
                log.error("审计日志写入失败", e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}