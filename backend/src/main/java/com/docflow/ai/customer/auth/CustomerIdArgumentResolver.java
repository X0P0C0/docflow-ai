package com.docflow.ai.customer.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 自定义参数解析器：从请求属性中提取客户ID。
 * 技术点：HandlerMethodArgumentResolver 自定义参数解析。
 */
public class CustomerIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CustomerId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object customerId = webRequest.getAttribute(CustomerAuthFilter.CUSTOMER_ID_ATTR, 0);
        if (customerId instanceof Long id) return id;
        if (customerId instanceof Number n) return n.longValue();
        if (customerId instanceof String s) return Long.parseLong(s);
        return null;
    }
}
