package com.docflow.ai.common.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2 模拟服务 —— 演示 OAuth2 授权流程
 * <p>
 * OAuth2 四种授权模式：
 * <ol>
 *   <li>授权码模式（Authorization Code）：最安全，第三方登录首选</li>
 *   <li>隐式模式（Implicit）：简化版，已不推荐</li>
 *   <li>密码模式（Password）：自家应用使用</li>
 *   <li>客户端凭证模式（Client Credentials）：服务间调用</li>
 * </ol>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>OAuth2 vs OIDC（OpenID Connect）的区别</li>
 *   <li>JWT vs Session Token 的取舍</li>
 *   <li>Token 刷新机制（Refresh Token）</li>
 *   <li>单点登录（SSO）实现原理</li>
 * </ul>
 */
@Slf4j
@Component
public class OAuth2Service {

    private static final String TOKEN_PREFIX = "oauth:token:";
    private static final String REFRESH_PREFIX = "oauth:refresh:";
    private static final long ACCESS_TOKEN_TTL = 3600;      // 1 小时
    private static final long REFRESH_TOKEN_TTL = 86400 * 7; // 7 天

    private final StringRedisTemplate redisTemplate;

    public OAuth2Service(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 授权码模式 - 生成授权码
     */
    public String generateAuthorizationCode(String clientId, String userId, String redirectUri) {
        String code = UUID.randomUUID().toString().replace("-", "");
        String codeKey = "oauth:code:" + code;

        String codeData = clientId + ":" + userId + ":" + redirectUri;
        redisTemplate.opsForValue().set(codeKey, codeData, 5, TimeUnit.MINUTES);

        log.info("Generated auth code for client: {}, user: {}", clientId, userId);
        return code;
    }

    /**
     * 授权码模式 - 用授权码换取 Token
     */
    public OAuth2Token exchangeCodeForToken(String code, String clientId, String clientSecret) {
        String codeKey = "oauth:code:" + code;
        String codeData = redisTemplate.opsForValue().get(codeKey);

        if (codeData == null) {
            throw new RuntimeException("授权码无效或已过期");
        }

        // 验证授权码
        redisTemplate.delete(codeKey);

        // 生成 Access Token 和 Refresh Token
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(TOKEN_PREFIX + accessToken, codeData, ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshToken, accessToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);

        return new OAuth2Token(accessToken, refreshToken, ACCESS_TOKEN_TTL);
    }

    /**
     * 验证 Token
     */
    public String validateToken(String accessToken) {
        String tokenData = redisTemplate.opsForValue().get(TOKEN_PREFIX + accessToken);
        return tokenData;
    }

    /**
     * 刷新 Token
     */
    public OAuth2Token refreshToken(String refreshToken) {
        String oldAccessToken = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        if (oldAccessToken == null) {
            throw new RuntimeException("Refresh Token 无效或已过期");
        }

        // 删除旧 Token
        redisTemplate.delete(TOKEN_PREFIX + oldAccessToken);
        redisTemplate.delete(REFRESH_PREFIX + refreshToken);

        // 生成新 Token
        String newAccessToken = UUID.randomUUID().toString().replace("-", "");
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");

        String tokenData = redisTemplate.opsForValue().get(TOKEN_PREFIX + oldAccessToken);
        if (tokenData == null) {
            tokenData = "refreshed";
        }

        redisTemplate.opsForValue().set(TOKEN_PREFIX + newAccessToken, tokenData, ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + newRefreshToken, newAccessToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);

        return new OAuth2Token(newAccessToken, newRefreshToken, ACCESS_TOKEN_TTL);
    }

    @Data
    public static class OAuth2Token {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn;
    }
}
