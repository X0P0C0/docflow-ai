package com.docflow.ai.customer.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(3)
public class CustomerAuthFilter implements Filter {

    private final CustomerTokenStore tokenStore;

    public CustomerAuthFilter(@Autowired(required = false) CustomerTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public static final String CUSTOMER_ID_ATTR = "customerId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (tokenStore == null) {
            chain.doFilter(request, response);
            return;
        }

        String uri = httpRequest.getRequestURI();
        if (!uri.startsWith("/api/customer/") || uri.contains("/login") || uri.contains("/register")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":40100,\"error\":\"AUTH_UNAUTHORIZED\",\"message\":\"Missing customer token\"}");
            return;
        }

        String token = authHeader.substring(7);
        Long customerId = tokenStore.getCustomerId(token);
        if (customerId == null) {
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":40100,\"error\":\"AUTH_UNAUTHORIZED\",\"message\":\"Invalid customer token\"}");
            return;
        }

        httpRequest.setAttribute(CUSTOMER_ID_ATTR, customerId);
        chain.doFilter(request, response);
    }
}
