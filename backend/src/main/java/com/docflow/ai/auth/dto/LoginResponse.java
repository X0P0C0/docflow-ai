package com.docflow.ai.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private Long expireSeconds;

    private CurrentUserResponse user;
}
