package com.docflow.ai.common.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based CSRF Token 存储
 * <p>
 * 原理：
 * <ul>
 *   <li>每次用户访问页面时，生成一个随机 Token</li>
 *   <li>Token 存储在 Redis，过期时间 2 小时</li>
 *   <li>前端在 POST/PUT/DELETE 请求中携带 Token</li>
 *   <li>后端验证 Token 是否匹配</li>
 * </ul>
 * <p>
 * 适用场景：
 * <ul>
 *   <li>前后端分离项目，Token 通过 Header 传递</li>
 *   <li>防止跨站请求伪造攻击</li>
 * </ul>
 */
@Component
public class RedisCsrfTokenRepository implements CsrfTokenRepository {

    private static final String CSRF_TOKEN_PREFIX = "csrf:token:";
    private static final long TOKEN_EXPIRE_HOURS = 2;

    private final StringRedisTemplate redisTemplate;

    public RedisCsrfTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        String headerName = "X-CSRF-TOKEN";
        String parameterName = "_csrf";
        return new DefaultCsrfToken(headerName, parameterName, tokenId);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String key = buildKey(request);
        if (token == null) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, token.getToken(), TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        String key = buildKey(request);
        String token = redisTemplate.opsForValue().get(key);
        if (token == null) {
            return null;
        }
        return new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", token);
    }

    private String buildKey(HttpServletRequest request) {
        String sessionId = request.getSession(false) != null
                ? request.getSession().getId()
                : request.getRemoteAddr();
        return CSRF_TOKEN_PREFIX + sessionId;
    }
}
