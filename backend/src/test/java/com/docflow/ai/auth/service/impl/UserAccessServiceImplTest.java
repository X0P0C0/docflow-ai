package com.docflow.ai.auth.service.impl;

import com.docflow.ai.auth.constant.PermissionCodes;
import com.docflow.ai.auth.constant.RoleCodes;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    private UserAccessServiceImpl userAccessService;

    @BeforeEach
    void setUp() {
        userAccessService = new UserAccessServiceImpl(sysUserMapper);
    }

    @Test
    void requireKnowledgeManagerShouldAllowPermissionBasedKnowledgeManager() {
        SysUser activeUser = activeUser(9L);
        when(sysUserMapper.selectById(9L)).thenReturn(activeUser);
        when(sysUserMapper.selectRoleCodesByUserId(9L)).thenReturn(List.of(RoleCodes.USER));
        when(sysUserMapper.selectPermissionCodesByUserId(9L))
                .thenReturn(List.of(PermissionCodes.KNOWLEDGE_ARTICLE_CREATE));

        assertDoesNotThrow(() -> userAccessService.requireKnowledgeManager(9L));
        assertTrue(userAccessService.canManageKnowledge(9L));
        assertFalse(userAccessService.canOperateTickets(9L));
        assertFalse(userAccessService.canAccessAiCenter(9L));
    }

    @Test
    void requireAiCenterAccessShouldRejectTicketPermissionWithoutAiRole() {
        SysUser activeUser = activeUser(7L);
        when(sysUserMapper.selectById(7L)).thenReturn(activeUser);
        when(sysUserMapper.selectRoleCodesByUserId(7L)).thenReturn(List.of(RoleCodes.USER));
        when(sysUserMapper.selectPermissionCodesByUserId(7L))
                .thenReturn(List.of(PermissionCodes.TICKET_ASSIGN));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userAccessService.requireAiCenterAccess(7L));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        assertTrue(userAccessService.canOperateTickets(7L));
        assertFalse(userAccessService.canAccessAiCenter(7L));
    }

    @Test
    void requireAiCenterAccessShouldAllowSystemManagerWithoutSupportRole() {
        SysUser activeUser = activeUser(3L);
        when(sysUserMapper.selectById(3L)).thenReturn(activeUser);
        when(sysUserMapper.selectRoleCodesByUserId(3L)).thenReturn(List.of(RoleCodes.USER));
        when(sysUserMapper.selectPermissionCodesByUserId(3L))
                .thenReturn(List.of(PermissionCodes.SYSTEM_USER_MANAGE));

        assertDoesNotThrow(() -> userAccessService.requireAiCenterAccess(3L));
        assertTrue(userAccessService.canAccessAiCenter(3L));
    }

    private SysUser activeUser(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}
