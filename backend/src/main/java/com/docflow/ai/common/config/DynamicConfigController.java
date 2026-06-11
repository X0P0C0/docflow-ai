package com.docflow.ai.common.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 动态配置管理接口
 * <p>
 * 提供运行时配置管理能力，无需重启服务
 */
@Tag(name = "动态配置", description = "运行时配置管理")
@RestController
@RequestMapping("/api/admin/config")
public class DynamicConfigController {

    private final DynamicConfigService configService;

    public DynamicConfigController(DynamicConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "获取配置项")
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", configService.get(key).orElse(null));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "设置配置项")
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> setConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        configService.set(key, value);
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("message", "配置已更新");
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取功能开关")
    @GetMapping("/feature/{featureName}")
    public ResponseEntity<Map<String, Object>> getFeatureFlag(@PathVariable String featureName) {
        Map<String, Object> result = new HashMap<>();
        result.put("feature", featureName);
        result.put("enabled", configService.getFeatureFlag(featureName, false));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "设置功能开关")
    @PutMapping("/feature/{featureName}")
    public ResponseEntity<Map<String, Object>> setFeatureFlag(
            @PathVariable String featureName,
            @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", false);
        configService.setFeatureFlag(featureName, enabled);
        Map<String, Object> result = new HashMap<>();
        result.put("feature", featureName);
        result.put("enabled", enabled);
        result.put("message", "功能开关已更新");
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取所有配置 keys")
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> getAllKeys() {
        Set<String> keys = configService.getAllKeys();
        Map<String, Object> result = new HashMap<>();
        result.put("keys", keys);
        result.put("count", keys != null ? keys.size() : 0);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "清除本地缓存")
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        configService.clearLocalCache();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "本地缓存已清除");
        return ResponseEntity.ok(result);
    }
}
