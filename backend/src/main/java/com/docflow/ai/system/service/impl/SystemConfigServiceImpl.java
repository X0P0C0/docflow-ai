package com.docflow.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.system.dto.SysConfigResponse;
import com.docflow.ai.system.dto.UpdateConfigRequest;
import com.docflow.ai.system.entity.SysConfig;
import com.docflow.ai.system.mapper.SysConfigMapper;
import com.docflow.ai.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SysConfigMapper configMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<SysConfigResponse> listConfigs(Long userId, String type) {
        userAccessService.requireSystemAdmin(userId);
        LambdaQueryWrapper<SysConfig> w = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) w.eq(SysConfig::getConfigType, type);
        w.orderByAsc(SysConfig::getConfigType, SysConfig::getConfigKey);
        return configMapper.selectList(w).stream().map(this::toResponse).toList();
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        LambdaQueryWrapper<SysConfig> w = new LambdaQueryWrapper<>();
        w.eq(SysConfig::getConfigKey, key).eq(SysConfig::getStatus, 1);
        SysConfig config = configMapper.selectOne(w);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfigResponse updateConfig(Long userId, UpdateConfigRequest request) {
        userAccessService.requireSystemAdmin(userId);
        LambdaQueryWrapper<SysConfig> w = new LambdaQueryWrapper<>();
        w.eq(SysConfig::getConfigKey, request.getConfigKey());
        SysConfig config = configMapper.selectOne(w);
        if (config != null) {
            config.setConfigValue(request.getConfigValue());
            if (request.getDescription() != null) config.setDescription(request.getDescription());
            config.setUpdateBy(userId);
            configMapper.updateById(config);
        } else {
            config = new SysConfig();
            config.setConfigKey(request.getConfigKey());
            config.setConfigValue(request.getConfigValue());
            config.setConfigType("CUSTOM");
            config.setDescription(request.getDescription());
            config.setStatus(1);
            config.setCreateBy(userId);
            configMapper.insert(config);
        }
        return toResponse(config);
    }

    private SysConfigResponse toResponse(SysConfig c) {
        SysConfigResponse r = new SysConfigResponse();
        r.setId(c.getId());
        r.setConfigKey(c.getConfigKey());
        r.setConfigValue(c.getConfigValue());
        r.setConfigType(c.getConfigType());
        r.setDescription(c.getDescription());
        r.setStatus(c.getStatus());
        r.setCreateTime(c.getCreateTime());
        return r;
    }
}
