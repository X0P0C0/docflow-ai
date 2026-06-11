package com.docflow.ai.common.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 主库强制切面 —— 拦截 @ForceMaster 注解
 */
@Slf4j
@Aspect
@Component
public class ForceMasterAspect {

    @Around("@annotation(forceMaster)")
    public Object forceMaster(ProceedingJoinPoint joinPoint, ForceMaster forceMaster) throws Throwable {
        DynamicDataSource.useMaster();
        try {
            return joinPoint.proceed();
        } finally {
            DynamicDataSource.clear();
        }
    }
}
