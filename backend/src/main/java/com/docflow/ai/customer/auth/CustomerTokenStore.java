package com.docflow.ai.customer.auth;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户令牌存储。
 * 技术点：简单的内存令牌管理，生产环境应使用 Redis。
 */
@Component
public class CustomerTokenStore {

    private final ConcurrentHashMap<String, Long> tokenToCustomerId = new ConcurrentHashMap<>();

    public String createToken(Long customerId) {
        String token = "customer-" + java.util.UUID.randomUUID().toString().replace("-", "");
        tokenToCustomerId.put(token, customerId);
        return token;
    }

    public Long getCustomerId(String token) {
        return tokenToCustomerId.get(token);
    }

    public void invalidate(String token) {
        tokenToCustomerId.remove(token);
    }
}
