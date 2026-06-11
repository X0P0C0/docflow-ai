package com.docflow.ai.config;

import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import com.docflow.ai.common.observability.ApiPerformanceInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimiterInterceptor rateLimiterInterceptor;

    private final ApiPerformanceInterceptor performanceInterceptor;

    public WebMvcConfig(@Autowired(required = false) RateLimiterInterceptor rateLimiterInterceptor,
                        ApiPerformanceInterceptor performanceInterceptor) {
        this.rateLimiterInterceptor = rateLimiterInterceptor;
        this.performanceInterceptor = performanceInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**");
        if (rateLimiterInterceptor != null) {
            registry.addInterceptor(rateLimiterInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html"
                );
        }
    }
}

