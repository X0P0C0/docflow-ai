package com.docflow.ai.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 - Spring Security 的核心组件
 *
 * 【面试考点】（JWT + Spring Security 是面试高频）
 *   - 过滤器职责单一：只负责"把 Token 还原成当前用户身份"
 *   - 具体能不能访问某个业务资源，交给后续的权限检查（@PreAuthorize 等）
 *   - OncePerRequestFilter：保证每次请求只执行一次（Spring 的防重复机制）
 *   - SecurityContextHolder：基于 ThreadLocal 存储用户信息，请求内共享
 *
 * 【认证流程】
 *   1. 从请求头提取 Bearer Token
 *   2. 验证 Token 有效性（签名 + 过期时间）
 *   3. 从 Token 解析出 userId、username、roles
 *   4. 构建 Authentication 对象放入 SecurityContext
 *   5. 后续的 @PreAuthorize 注解就能读取到当前用户信息
 *
 * 【安全要点】
 *   - Token 无效或过期：不设置 Authentication，后续会被权限拦截返回 401
 *   - 已有 Authentication：不重复设置（防止覆盖）
 *   - 前后端统一约定 Bearer Token 格式，避免多种登录态传递方式
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 只有 Token 有效且当前没有已认证用户时，才设置 SecurityContext
        if (StringUtils.hasText(token)
                && jwtTokenProvider.isValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);

            // 从 Token 解析角色，加 "ROLE_" 前缀（Spring Security 的约定）
            // hasRole("ADMIN") 实际检查的是 "ROLE_ADMIN" 权限字符串
            List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getRoles(token).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            // 构建认证对象（userId + username 封装为 AuthUserPrincipal）
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            new AuthUserPrincipal(userId, username),
                            null,  // 不需要密码（已通过 JWT 认证）
                            authorities
                    );
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // 无论是否认证成功，都继续执行过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 Token
     * 约定格式：Authorization: Bearer <token>
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7); // 跳过 "Bearer " 前缀
    }
}