package com.docflow.ai.ticket.service.impl;

import com.docflow.ai.ticket.dto.SlaPolicyRequest;
import com.docflow.ai.ticket.entity.SlaPolicy;
import com.docflow.ai.ticket.service.SlaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SlaServiceTest {

    @Autowired
    private SlaService slaService;

    @Test
    @DisplayName("Create SLA policy with valid data")
    void createPolicy_validData_success() {
        SlaPolicyRequest request = new SlaPolicyRequest();
        request.setName("Test SLA");
        request.setPriority(2);
        request.setResponseHours(2);
        request.setResolveHours(24);

        SlaPolicy policy = slaService.createPolicy(1L, request);

        assertNotNull(policy);
        assertEquals("Test SLA", policy.getName());
        assertEquals(2, policy.getPriority());
        assertEquals(2, policy.getResponseHours());
        assertEquals(24, policy.getResolveHours());
    }

    @Test
    @DisplayName("List all SLA policies")
    void listPolicies_returnsList() {
        List<SlaPolicy> policies = slaService.listPolicies();
        assertNotNull(policies);
    }

    @Test
    @DisplayName("Delete SLA policy")
    void deletePolicy_validId_success() {
        SlaPolicyRequest request = new SlaPolicyRequest();
        request.setName("To Delete");
        request.setPriority(1);
        request.setResponseHours(1);
        request.setResolveHours(1);
        SlaPolicy created = slaService.createPolicy(1L, request);

        assertDoesNotThrow(() -> slaService.deletePolicy(created.getId(), 1L));
    }
}
