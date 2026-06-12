package com.docflow.ai.config;

import com.docflow.ai.auth.security.JwtAuthenticationFilter;
import com.docflow.ai.auth.security.RestAccessDeniedHandler;
import com.docflow.ai.auth.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置 - 定义认证和授权规则
 *
 * 【面试考点】（Spring Security 是 Java Web 安全的核心框架）
 *
 * 【核心概念】
 *   - Authentication（认证）：你是谁？（JWT Token -> 用户身份）
 *   - Authorization（授权）：你能做什么？（角色/权限 -> 资源访问）
 *   - CSRF（跨站请求伪造）：前后端分离 + JWT 不需要 CSRF 保护
 *   - Session：无状态架构，不使用 HttpSession
 *
 * 【过滤器链】
 *   请求 -> JwtAuthenticationFilter -> UsernamePasswordAuthenticationFilter -> Controller
 *   JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前执行
 *   负责从 Token 解析用户身份，放入 SecurityContext
 *
 * 【公开端点】（不需要认证即可访问）
 *   /api/health        - 健康检查
 *   /api/auth/login    - 登录接口
 *   /api/customer/**   - 客户门户（独立认证体系）
 *   /swagger-ui/**     - API 文档
 *   /actuator/**       - 监控端点
 *
 * 【密码加密】
 *   BCryptPasswordEncoder：业界标准的密码哈希算法
 *   每次生成的哈希值不同（包含随机盐），彩虹表攻击无效
 *   验证时从哈希中提取盐值进行比对
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 这是纯 API 后端：不走服务端 session，也不走表单登录
                // 【面试考点】前后端分离项目必须禁用 CSRF（Token 认证不需要）
                .csrf(csrf -> csrf.disable())

                // 自定义异常处理：返回 JSON 而非 HTML 错误页
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)  // 401: 未认证
                        .accessDeniedHandler(restAccessDeniedHandler))           // 403: 无权限

                // 无状态：不创建 HttpSession（JWT 自带用户信息）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL 级别的权限控制
                .authorizeHttpRequests(auth -> auth
                        // 公开端点：允许匿名访问
                        .requestMatchers(
                                "/api/health",           // 健康检查
                                "/api/auth/login",       // 登录
                                "/api/auth/refresh",     // 刷新 Token
                                "/api/customer/**",      // 客户门户（独立认证）
                                "/swagger-ui.html",      // Swagger UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"           // Spring Boot 监控
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS 预检请求
                        .anyRequest().authenticated()  // 其他所有请求必须认证
                )

                // 在进入 controller 之前先把 JWT 解析进 SecurityContext
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 密码算法集中在配置里，业务层只依赖 PasswordEncoder 接口
        // 【面试考点】为什么用 BCrypt？
        //   - 自带随机盐，每次哈希结果不同
        //   - 计算慢（故意的），防暴力破解
        //   - 不可逆，数据库泄露也无法还原密码
        return new BCryptPasswordEncoder();
    }
}