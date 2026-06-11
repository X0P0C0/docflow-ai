package com.docflow.ai.system.service;

import com.docflow.ai.system.dto.AutomationRuleResponse;
import com.docflow.ai.system.dto.CreateRuleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AutomationRuleServiceTest {

    @Autowired
    private AutomationRuleService ruleService;

    @Test
    @DisplayName("List automation rules")
    void listRules_validUser_returnsList() {
        List<AutomationRuleResponse> rules = ruleService.listRules(1L);
        assertNotNull(rules);
    }

    @Test
    @DisplayName("Create automation rule")
    void createRule_validData_success() {
        CreateRuleRequest request = new CreateRuleRequest();
        request.setName("Test Rule");
        request.setDescription("Test description");
        request.setTriggerType("TICKET_CREATED");
        request.setConditions("{\"priority\": 4}");
        request.setActions("{\"action\": \"ESCALATE\"}");
        request.setPriority(10);

        AutomationRuleResponse rule = ruleService.createRule(1L, request);

        assertNotNull(rule);
        assertEquals("Test Rule", rule.getName());
        assertEquals("TICKET_CREATED", rule.getTriggerType());
    }

    @Test
    @DisplayName("Toggle automation rule")
    void toggleRule_validId_success() {
        List<AutomationRuleResponse> rules = ruleService.listRules(1L);
        if (!rules.isEmpty()) {
            assertDoesNotThrow(() -> ruleService.toggleRule(rules.get(0).getId(), 1L, 0));
        }
    }

    @Test
    @DisplayName("Delete automation rule")
    void deleteRule_validId_success() {
        CreateRuleRequest request = new CreateRuleRequest();
        request.setName("To Delete");
        request.setTriggerType("STATUS_CHANGED");
        request.setConditions("{}");
        request.setActions("{}");
        AutomationRuleResponse created = ruleService.createRule(1L, request);

        assertDoesNotThrow(() -> ruleService.deleteRule(created.getId(), 1L));
    }
}
