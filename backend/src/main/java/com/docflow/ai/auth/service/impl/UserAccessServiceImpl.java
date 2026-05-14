package com.docflow.ai.auth.service.impl;

import com.docflow.ai.auth.constant.CapabilityCodes;
import com.docflow.ai.auth.constant.PermissionCodes;
import com.docflow.ai.auth.constant.RoleCodes;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service("userAccessService")
@RequiredArgsConstructor
public class UserAccessServiceImpl implements UserAccessService {

    private static final Set<String> TICKET_OPERATOR_ROLE_CODES = Set.of(RoleCodes.ADMIN, RoleCodes.SUPPORT);
    private static final Set<String> KNOWLEDGE_MANAGER_PERMISSION_CODES = Set.of(PermissionCodes.KNOWLEDGE_ARTICLE_CREATE);
    private static final Set<String> TICKET_OPERATOR_PERMISSION_CODES = Set.of(PermissionCodes.TICKET_ASSIGN);
    private static final Set<String> SYSTEM_MANAGER_PERMISSION_CODES = Set.of(
            PermissionCodes.SYSTEM_USER_MANAGE,
            PermissionCodes.SYSTEM_ROLE_MANAGE
    );

    private final SysUserMapper sysUserMapper;

    @Override
    public SysUser requireActiveUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    @Override
    public void requireTicketOperator(Long userId) {
        requireActiveUser(userId);
        if (!canOperateTickets(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @Override
    public boolean canOperateTickets(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleCodes = getRoleCodes(userId);
        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(userId);
        return canOperateTickets(roleCodes, permissionCodes);
    }

    @Override
    public boolean canManageKnowledge(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleCodes = getRoleCodes(userId);
        List<String> permissionCodes = sysUserMapper.selectPermissionCodesByUserId(userId);
        return canManageKnowledge(roleCodes, permissionCodes);
    }

    @Override
    public boolean canManageKnowledge(List<String> roleCodes, List<String> permissionCodes) {
        return containsAny(permissionCodes, KNOWLEDGE_MANAGER_PERMISSION_CODES)
                || containsAny(roleCodes, TICKET_OPERATOR_ROLE_CODES);
    }

    @Override
    public boolean canOperateTickets(List<String> roleCodes, List<String> permissionCodes) {
        return containsAny(permissionCodes, TICKET_OPERATOR_PERMISSION_CODES)
                || containsAny(roleCodes, TICKET_OPERATOR_ROLE_CODES);
    }

    @Override
    public boolean canManageSystem(List<String> roleCodes, List<String> permissionCodes) {
        return containsAny(permissionCodes, SYSTEM_MANAGER_PERMISSION_CODES)
                || containsAny(roleCodes, Set.of(RoleCodes.ADMIN));
    }

    @Override
    public boolean canAccessAiCenter(List<String> roleCodes, List<String> permissionCodes) {
        return containsAny(roleCodes, TICKET_OPERATOR_ROLE_CODES);
    }

    @Override
    public List<String> resolveCapabilities(List<String> roleCodes, List<String> permissionCodes) {
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        if (canOperateTickets(roleCodes, permissionCodes)) {
            capabilities.add(CapabilityCodes.TICKET_OPERATE);
            capabilities.add(CapabilityCodes.TICKET_ASSIGN);
            capabilities.add(CapabilityCodes.TICKET_TRANSITION);
            capabilities.add(CapabilityCodes.TICKET_INTERNAL_COMMENT);
            capabilities.add(CapabilityCodes.TICKET_VIEW_ALL);
        }
        if (canManageKnowledge(roleCodes, permissionCodes)) {
            capabilities.add(CapabilityCodes.KNOWLEDGE_MANAGE);
        }
        if (canAccessAiCenter(roleCodes, permissionCodes)) {
            capabilities.add(CapabilityCodes.AI_CENTER_ACCESS);
        }
        if (canManageSystem(roleCodes, permissionCodes)) {
            capabilities.add(CapabilityCodes.SYSTEM_MANAGE);
        }
        return List.copyOf(capabilities);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return sysUserMapper.selectRoleCodesByUserId(userId);
    }

    private boolean containsAny(List<String> values, Set<String> expectedValues) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream().anyMatch(expectedValues::contains);
    }
}
