package com.docflow.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.system.dto.AutomationRuleResponse;
import com.docflow.ai.system.dto.CreateRuleRequest;
import com.docflow.ai.system.entity.AutomationRule;
import com.docflow.ai.system.mapper.AutomationRuleMapper;
import com.docflow.ai.system.service.AutomationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutomationRuleServiceImpl implements AutomationRuleService {

    private final AutomationRuleMapper ruleMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<AutomationRuleResponse> listRules(Long userId) {
        userAccessService.requireSystemAdmin(userId);
        return ruleMapper.selectList(new LambdaQueryWrapper<AutomationRule>()
                .eq(AutomationRule::getDeleted, 0)
                .orderByDesc(AutomationRule::getPriority))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutomationRuleResponse createRule(Long userId, CreateRuleRequest request) {
        userAccessService.requireSystemAdmin(userId);
        AutomationRule rule = new AutomationRule();
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerType(request.getTriggerType());
        rule.setConditions(request.getConditions());
        rule.setActions(request.getActions());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        rule.setDeleted(0);
        rule.setCreateBy(userId);
        ruleMapper.insert(rule);
        return toResponse(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutomationRuleResponse updateRule(Long id, Long userId, CreateRuleRequest request) {
        userAccessService.requireSystemAdmin(userId);
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Rule not found");
        }
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerType(request.getTriggerType());
        rule.setConditions(request.getConditions());
        rule.setActions(request.getActions());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getStatus() != null) rule.setStatus(request.getStatus());
        rule.setUpdateBy(userId);
        ruleMapper.updateById(rule);
        return toResponse(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id, Long userId) {
        userAccessService.requireSystemAdmin(userId);
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        rule.setDeleted(1);
        rule.setUpdateBy(userId);
        ruleMapper.updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRule(Long id, Long userId, Integer status) {
        userAccessService.requireSystemAdmin(userId);
        AutomationRule rule = ruleMapper.selectById(id);
        if (rule == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        rule.setStatus(status);
        rule.setUpdateBy(userId);
        ruleMapper.updateById(rule);
    }

    private AutomationRuleResponse toResponse(AutomationRule r) {
        AutomationRuleResponse resp = new AutomationRuleResponse();
        resp.setId(r.getId());
        resp.setName(r.getName());
        resp.setDescription(r.getDescription());
        resp.setTriggerType(r.getTriggerType());
        resp.setConditions(r.getConditions());
        resp.setActions(r.getActions());
        resp.setPriority(r.getPriority());
        resp.setStatus(r.getStatus());
        resp.setCreateTime(r.getCreateTime());
        return resp;
    }
}
