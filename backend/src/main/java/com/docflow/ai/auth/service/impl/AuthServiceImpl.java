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
import com.docflow.ai.monitoring.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final BusinessMetricsService metrics;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse login(String username, String password) {
        // 登录链路：校验账号 -> 解析权限 -> 回写最后登录时间 -> 生成 JWT -> 返回前端所需用户画像。
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
        response.setRefreshToken(jwtTokenProvider.createToken(user.getId(), user.getUsername(), roles));
        response.setRefreshTokenExpireSeconds(jwtTokenProvider.getExpireSeconds() * 7);
        metrics.loginAttempt("success");
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
        // 不区分“用户名不存在”和“密码错误”的返回细节，避免泄露账号存在性。
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return user;
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // Validate the refresh token
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // Check if token was already used (single-use)
        String usedKey = "auth:refresh:used:" + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(usedKey))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // Mark as used
        redisTemplate.opsForValue().set(usedKey, "1", java.time.Duration.ofDays(7));

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);

        LoginResponse response = new LoginResponse();
        response.setToken(jwtTokenProvider.createToken(userId, username, roles));
        response.setExpireSeconds(jwtTokenProvider.getExpireSeconds());
        response.setRefreshToken(jwtTokenProvider.createToken(userId, username, roles));
        response.setRefreshTokenExpireSeconds(jwtTokenProvider.getExpireSeconds() * 7);

        SysUser user = sysUserMapper.selectById(userId);
        response.setUser(toCurrentUser(user, roles, sysUserMapper.selectPermissionCodesByUserId(userId)));

        return response;
    }

    private CurrentUserResponse toCurrentUser(SysUser user, List<String> roles, List<String> permissions) {
        // capabilities 不是数据库直存字段，而是给前端消费的“聚合能力视图”。
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
