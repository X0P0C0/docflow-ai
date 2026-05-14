package com.docflow.ai.auth.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthUserPrincipal {

    private final Long userId;

    private final String username;
}
