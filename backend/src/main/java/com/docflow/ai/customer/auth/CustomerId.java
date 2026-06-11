package com.docflow.ai.customer.auth;

import java.lang.annotation.*;

/**
 * 客户ID参数注解。
 * 标注在Controller方法参数上，自动从请求属性中注入客户ID。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomerId {
}
