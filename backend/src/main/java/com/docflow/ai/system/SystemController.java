package com.docflow.ai.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.entity.SysDept;
import com.docflow.ai.auth.entity.SysRole;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysDeptMapper;
import com.docflow.ai.auth.mapper.SysRoleMapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.common.domain.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    @GetMapping("/users")
    public ApiResponse<List<UserItem>> listUsers() {
        List<SysUser> users = userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0).orderByAsc(SysUser::getId)
        );
        List<UserItem> items = users.stream().map(u -> {
            UserItem item = new UserItem();
            item.setId(u.getId());
            item.setUsername(u.getUsername());
            item.setNickname(u.getNickname());
            item.setRealName(u.getRealName());
            item.setEmail(u.getEmail());
            item.setPhone(u.getPhone());
            item.setStatus(u.getStatus());
            item.setDeptId(u.getDeptId());
            item.setLastLoginTime(u.getLastLoginTime());
            return item;
        }).collect(Collectors.toList());
        return ApiResponse.success(items);
    }

    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> listRoles() {
        List<SysRole> roles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0).orderByAsc(SysRole::getId)
        );
        return ApiResponse.success(roles);
    }

    @GetMapping("/depts")
    public ApiResponse<List<SysDept>> listDepts() {
        List<SysDept> depts = deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0).orderByAsc(SysDept::getSortOrder)
        );
        return ApiResponse.success(depts);
    }

    @lombok.Data
    public static class UserItem {
        private Long id;
        private String username;
        private String nickname;
        private String realName;
        private String email;
        private String phone;
        private Integer status;
        private Long deptId;
        private java.time.LocalDateTime lastLoginTime;
    }
}
