package com.docflow.ai.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    // token 是前端后续所有受保护 API 的唯一登录态凭证。
    private String token;

    // 过期时间显式返回给前端，方便后续做会话提示、续期或失效兜底。
    private Long expireSeconds;

    // 登录成功后把当前用户画像一起返回，前端首屏无需再额外拼装角色/能力信息。
    private String refreshToken;
    private Long refreshTokenExpireSeconds;
    private CurrentUserResponse user;
}
