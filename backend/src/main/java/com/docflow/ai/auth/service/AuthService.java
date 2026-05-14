package com.docflow.ai.auth.service;

import com.docflow.ai.auth.dto.CurrentUserResponse;
import com.docflow.ai.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(String username, String password);

    CurrentUserResponse getCurrentUser(Long userId);
}
