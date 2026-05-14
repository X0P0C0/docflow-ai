package com.docflow.ai.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docflow.ai.auth.constant.RoleCodes;
import com.docflow.ai.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select({
            "SELECT r.role_code",
            "FROM sys_role r",
            "INNER JOIN sys_user_role ur ON ur.role_id = r.id",
            "WHERE ur.user_id = #{userId}",
            "AND r.deleted = 0",
            "AND r.status = 1",
            "ORDER BY r.id"
    })
    List<String> selectRoleCodesByUserId(Long userId);

    @Select({
            "SELECT DISTINCT p.permission_code",
            "FROM sys_permission p",
            "INNER JOIN sys_role_permission rp ON rp.permission_id = p.id",
            "INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id",
            "INNER JOIN sys_role r ON r.id = ur.role_id",
            "WHERE ur.user_id = #{userId}",
            "AND p.deleted = 0",
            "AND p.status = 1",
            "AND r.deleted = 0",
            "AND r.status = 1",
            "ORDER BY p.permission_code"
    })
    List<String> selectPermissionCodesByUserId(Long userId);

    @Select({
            "SELECT DISTINCT u.*",
            "FROM sys_user u",
            "INNER JOIN sys_user_role ur ON ur.user_id = u.id",
            "INNER JOIN sys_role r ON r.id = ur.role_id",
            "WHERE u.deleted = 0",
            "AND u.status = 1",
            "AND r.deleted = 0",
            "AND r.status = 1",
            "AND r.role_code IN ('" + RoleCodes.ADMIN + "', '" + RoleCodes.SUPPORT + "')",
            "ORDER BY u.id"
    })
    List<SysUser> selectAssignableUsers();
}
