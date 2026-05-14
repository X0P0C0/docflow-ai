package com.docflow.ai.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class CurrentUserResponse {

    private Long id;

    private String username;

    private String nickname;

    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private List<String> roles;

    private List<String> permissions;

    private List<String> capabilities;
}
