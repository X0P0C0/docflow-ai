package com.docflow.ai.system.service;

import com.docflow.ai.system.dto.SysConfigResponse;
import com.docflow.ai.system.dto.UpdateConfigRequest;
import java.util.List;

public interface SystemConfigService {
    List<SysConfigResponse> listConfigs(Long userId, String type);
    String getConfigValue(String key, String defaultValue);
    SysConfigResponse updateConfig(Long userId, UpdateConfigRequest request);
}
