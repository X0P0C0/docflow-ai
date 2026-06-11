package com.docflow.ai.common.api;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API 版本拦截器 —— 支持多种版本号传递方式
 * <p>
 * 优先级：
 * <ol>
 *   <li>Accept-Version Header</li>
 *   <li>URL 路径 /api/v1/...</li>
 *   <li>Query 参数 ?version=1</li>
 * </ol>
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final String VERSION_HEADER = "Accept-Version";
    private static final String VERSION_QUERY = "version";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        ApiVersion methodVersion = handlerMethod.getMethodAnnotation(ApiVersion.class);
        ApiVersion classVersion = handlerMethod.getBeanType().getAnnotation(ApiVersion.class);

        int requiredVersion = methodVersion != null ? methodVersion.value()
                : (classVersion != null ? classVersion.value() : 1);

        int requestVersion = extractVersion(request);

        // 如果请求版本不匹配，返回 404
        if (requestVersion > 0 && requestVersion != requiredVersion) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        // 设置响应头，标记当前 API 版本
        response.setHeader("X-API-Version", String.valueOf(requiredVersion));

        return true;
    }

    private int extractVersion(HttpServletRequest request) {
        // 1. Check header
        String versionHeader = request.getHeader(VERSION_HEADER);
        if (versionHeader != null) {
            try {
                return Integer.parseInt(versionHeader);
            } catch (NumberFormatException ignored) {}
        }

        // 2. Check URL path: /api/v1/...
        String path = request.getRequestURI();
        int vIndex = path.indexOf("/v");
        if (vIndex > 0) {
            int nextSlash = path.indexOf("/", vIndex + 2);
            String versionStr = nextSlash > 0 ? path.substring(vIndex + 2, nextSlash) : path.substring(vIndex + 2);
            try {
                return Integer.parseInt(versionStr);
            } catch (NumberFormatException ignored) {}
        }

        // 3. Check query parameter
        String versionQuery = request.getParameter(VERSION_QUERY);
        if (versionQuery != null) {
            try {
                return Integer.parseInt(versionQuery);
            } catch (NumberFormatException ignored) {}
        }

        return -1; // No version specified
    }
}
