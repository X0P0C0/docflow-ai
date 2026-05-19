package com.docflow.ai.integration;

import com.docflow.ai.auth.security.JwtTokenProvider;
import com.docflow.ai.support.AbstractContainerIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.List;

public abstract class AbstractApiContainerIntegrationTest extends AbstractContainerIntegrationTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    protected String bearerToken(Long userId, String username, String... roles) {
        return "Bearer " + jwtTokenProvider.createToken(userId, username, List.of(roles));
    }

    protected String authHeader(Long userId, String username, String... roles) {
        return bearerToken(userId, username, roles);
    }

    protected String authorizationHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }
}
