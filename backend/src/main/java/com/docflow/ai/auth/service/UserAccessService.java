package com.docflow.ai.auth.service;

import com.docflow.ai.auth.entity.SysUser;

import java.util.List;

public interface UserAccessService {

    SysUser requireActiveUser(Long userId);

    void requireTicketOperator(Long userId);

    boolean canOperateTickets(Long userId);

    boolean canManageKnowledge(Long userId);

    boolean canManageKnowledge(List<String> roleCodes, List<String> permissionCodes);

    boolean canOperateTickets(List<String> roleCodes, List<String> permissionCodes);

    boolean canManageSystem(List<String> roleCodes, List<String> permissionCodes);

    boolean canAccessAiCenter(List<String> roleCodes, List<String> permissionCodes);

    List<String> resolveCapabilities(List<String> roleCodes, List<String> permissionCodes);

    List<String> getRoleCodes(Long userId);
}
