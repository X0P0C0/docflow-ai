package com.docflow.ai.system.service;

import com.docflow.ai.system.dto.AutomationRuleResponse;
import com.docflow.ai.system.dto.CreateRuleRequest;
import java.util.List;

public interface AutomationRuleService {
    List<AutomationRuleResponse> listRules(Long userId);
    AutomationRuleResponse createRule(Long userId, CreateRuleRequest request);
    AutomationRuleResponse updateRule(Long id, Long userId, CreateRuleRequest request);
    void deleteRule(Long id, Long userId);
    void toggleRule(Long id, Long userId, Integer status);
}
