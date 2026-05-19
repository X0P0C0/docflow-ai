package com.docflow.ai.ai.service.impl;

import com.docflow.ai.ai.config.AiWorkspaceProperties;
import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AiWorkspaceServiceSpringBootTest.Config.class)
class AiWorkspaceServiceSpringBootTest {

    @Autowired
    private AiWorkspaceServiceImpl aiWorkspaceService;

    @MockBean
    private TicketMapper ticketMapper;

    @MockBean
    private TicketCommentMapper ticketCommentMapper;

    @MockBean
    private TicketRecordMapper ticketRecordMapper;

    @MockBean
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @MockBean
    private SysUserMapper sysUserMapper;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private SetOperations<String, String> setOperations;

    @MockBean
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    void markReplyDraftAdoptedShouldPersistAdoptionState() {
        Ticket ticket = new Ticket();
        ticket.setId(18L);
        ticket.setDeleted(0);

        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("support01");
        user.setRealName("Support Wang");
        user.setDeleted(0);
        user.setStatus(1);

        doNothing().when(userAccessService).requireAiCenterAccess(2L);
        when(ticketMapper.selectById(18L)).thenReturn(ticket);
        when(sysUserMapper.selectById(2L)).thenReturn(user);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        AiWorkspaceAdoptionResponse response = aiWorkspaceService.markReplyDraftAdopted(2L, 18L);

        assertThat(response.getTicketId()).isEqualTo(18L);
        assertThat(response.isAdopted()).isTrue();
        assertThat(response.getAdoptedByUserId()).isEqualTo(2L);
        assertThat(response.getAdoptedByName()).isEqualTo("Support Wang");
        assertThat(response.getAdoptedAt()).isNotNull();
        assertThat(response.getLastActivityAt()).isEqualTo(response.getAdoptedAt());
        assertThat(response.getClaimFreshness()).isEqualTo("fresh");

        verify(setOperations).add("docflow:ai:workspace:adoptedTickets", "18");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> fieldCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations, times(3)).put(keyCaptor.capture(), fieldCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getAllValues()).allMatch("docflow:ai:workspace:adoption:18"::equals);
        assertThat(fieldCaptor.getAllValues()).containsExactlyInAnyOrder("userId", "userName", "adoptedAt");
        assertThat(valueCaptor.getAllValues()).contains("2", "Support Wang");
    }

    @TestConfiguration
    static class Config {

        @Bean
        AiWorkspaceProperties aiWorkspaceProperties() {
            AiWorkspaceProperties properties = new AiWorkspaceProperties();
            properties.setClaimStaleAfter(Duration.ofHours(2));
            return properties;
        }

        @Bean
        AiWorkspaceServiceImpl aiWorkspaceService(TicketMapper ticketMapper,
                                                  TicketCommentMapper ticketCommentMapper,
                                                  TicketRecordMapper ticketRecordMapper,
                                                  KnowledgeArticleMapper knowledgeArticleMapper,
                                                  SysUserMapper sysUserMapper,
                                                  UserAccessService userAccessService,
                                                  StringRedisTemplate stringRedisTemplate,
                                                  AiWorkspaceProperties aiWorkspaceProperties) {
            return new AiWorkspaceServiceImpl(
                    ticketMapper,
                    ticketCommentMapper,
                    ticketRecordMapper,
                    knowledgeArticleMapper,
                    sysUserMapper,
                    userAccessService,
                    stringRedisTemplate,
                    aiWorkspaceProperties
            );
        }
    }
}
