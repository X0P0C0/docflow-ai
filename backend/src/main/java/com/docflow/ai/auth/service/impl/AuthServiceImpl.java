package com.docflow.ai.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.dto.CurrentUserResponse;
import com.docflow.ai.auth.dto.LoginResponse;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.security.JwtTokenProvider;
import com.docflow.ai.auth.service.AuthService;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccessService userAccessService;

    @Override
    public LoginResponse login(String username, String password) {
        SysUser user = findActiveUserByUsername(username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(user.getId());

        SysUser updateUser = new SysUser();
        updateUser.setId(user.getId());
        LocalDateTime now = LocalDateTime.now();
        updateUser.setLastLoginTime(now);
        updateUser.setUpdateBy(user.getId());
        updateUser.setUpdateTime(now);
        sysUserMapper.updateById(updateUser);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtTokenProvider.createToken(user.getId(), user.getUsername(), roles));
        response.setExpireSeconds(jwtTokenProvider.getExpireSeconds());
        response.setUser(toCurrentUser(user, roles, permissions));
        return response;
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        return toCurrentUser(
                user,
                sysUserMapper.selectRoleCodesByUserId(userId),
                sysUserMapper.selectPermissionCodesByUserId(userId)
        );
    }

    private SysUser findActiveUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("LIMIT 1");

        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return user;
    }

    private CurrentUserResponse toCurrentUser(SysUser user, List<String> roles, List<String> permissions) {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setRoles(roles);
        response.setPermissions(permissions);
        response.setCapabilities(userAccessService.resolveCapabilities(roles, permissions));
        return response;
    }
}
