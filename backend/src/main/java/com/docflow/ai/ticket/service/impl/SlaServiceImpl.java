package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.dto.SlaPolicyRequest;
import com.docflow.ai.ticket.dto.SlaStatusResponse;
import com.docflow.ai.ticket.entity.SlaPolicy;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.SlaPolicyMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.service.SlaService;
import com.docflow.ai.monitoring.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaPolicyMapper policyMapper;
    private final TicketMapper ticketMapper;
    private final UserAccessService userAccessService;
    private final BusinessMetricsService metrics;

    @Override
    public List<SlaPolicy> listPolicies() {
        return policyMapper.selectList(new LambdaQueryWrapper<SlaPolicy>()
                .eq(SlaPolicy::getStatus, 1)
                .orderByAsc(SlaPolicy::getTicketType)
                .orderByAsc(SlaPolicy::getPriority));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicy createPolicy(Long userId, SlaPolicyRequest request) {
        userAccessService.requireTicketOperator(userId);
        SlaPolicy policy = new SlaPolicy();
        policy.setName(request.getName());
        policy.setTicketType(request.getTicketType());
        policy.setPriority(request.getPriority());
        policy.setResponseHours(request.getResponseHours());
        policy.setResolveHours(request.getResolveHours());
        policy.setBusinessHoursOnly(request.getBusinessHoursOnly() != null ? request.getBusinessHoursOnly() : 0);
        policy.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        policyMapper.insert(policy);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicy updatePolicy(Long id, Long userId, SlaPolicyRequest request) {
        userAccessService.requireTicketOperator(userId);
        SlaPolicy policy = policyMapper.selectById(id);
        if (policy == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "SLA policy not found");
        policy.setName(request.getName());
        policy.setTicketType(request.getTicketType());
        policy.setPriority(request.getPriority());
        policy.setResponseHours(request.getResponseHours());
        policy.setResolveHours(request.getResolveHours());
        if (request.getBusinessHoursOnly() != null) policy.setBusinessHoursOnly(request.getBusinessHoursOnly());
        if (request.getStatus() != null) policy.setStatus(request.getStatus());
        policyMapper.updateById(policy);
        return policy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        SlaPolicy policy = policyMapper.selectById(id);
        if (policy == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "SLA policy not found");
        policy.setStatus(0);
        policyMapper.updateById(policy);
    }

    @Override
    public void calculateDeadlines(Long ticketId, String ticketType, Integer priority) {
        SlaPolicy policy = findBestMatch(ticketType, priority);
        if (policy == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setSlaResponseDeadline(now.plusHours(policy.getResponseHours()));
        update.setSlaResolveDeadline(now.plusHours(policy.getResolveHours()));
        ticketMapper.updateById(update);
    }

    @Override
    public SlaStatusResponse getSlaStatus(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        return buildSlaStatus(ticket);
    }

    @Override
    public List<SlaStatusResponse> getBreachedTickets(Long userId) {
        userAccessService.requireActiveUser(userId);
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Ticket::getStatus, List.of(1, 2));
        wrapper.and(w -> w
                .le(Ticket::getSlaResponseDeadline, now).and(x -> x.isNull(Ticket::getFirstResponseTime))
                .or()
                .le(Ticket::getSlaResolveDeadline, now).and(x -> x.ne(Ticket::getStatus, 3)));
        wrapper.last("LIMIT 100");
        List<Ticket> tickets = ticketMapper.selectList(wrapper);
        List<SlaStatusResponse> result = new ArrayList<>();
        for (Ticket t : tickets) {
            SlaStatusResponse s = buildSlaStatus(t);
            if (s.isResponseBreached() || s.isResolveBreached()) result.add(s);
                metrics.slaBreached();
        }
        return result;
    }

    private SlaPolicy findBestMatch(String ticketType, Integer priority) {
        LambdaQueryWrapper<SlaPolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaPolicy::getStatus, 1);
        wrapper.eq(SlaPolicy::getTicketType, ticketType);
        wrapper.eq(SlaPolicy::getPriority, priority);
        wrapper.last("LIMIT 1");
        SlaPolicy exact = policyMapper.selectOne(wrapper);
        if (exact != null) return exact;
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaPolicy::getStatus, 1);
        wrapper.eq(SlaPolicy::getTicketType, ticketType);
        wrapper.isNull(SlaPolicy::getPriority);
        wrapper.last("LIMIT 1");
        return policyMapper.selectOne(wrapper);
    }

    private SlaStatusResponse buildSlaStatus(Ticket ticket) {
        SlaStatusResponse s = new SlaStatusResponse();
        s.setTicketId(ticket.getId());
        s.setTicketNo(ticket.getTicketNo());
        s.setResponseDeadline(ticket.getSlaResponseDeadline());
        s.setResolveDeadline(ticket.getSlaResolveDeadline());
        s.setFirstResponseTime(ticket.getFirstResponseTime());
        s.setResolvedTime(ticket.getResolvedTime());
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getSlaResponseDeadline() != null) {
            if (ticket.getFirstResponseTime() != null) {
                s.setResponseBreached(ticket.getFirstResponseTime().isAfter(ticket.getSlaResponseDeadline()));
                s.setResponseRemainingMinutes(0);
            } else {
                s.setResponseBreached(now.isAfter(ticket.getSlaResponseDeadline()));
                s.setResponseRemainingMinutes(ChronoUnit.MINUTES.between(now, ticket.getSlaResponseDeadline()));
            }
        }
        if (ticket.getSlaResolveDeadline() != null) {
            if (ticket.getResolvedTime() != null) {
                s.setResolveBreached(ticket.getResolvedTime().isAfter(ticket.getSlaResolveDeadline()));
                s.setResolveRemainingMinutes(0);
            } else {
                s.setResolveBreached(now.isAfter(ticket.getSlaResolveDeadline()));
                s.setResolveRemainingMinutes(ChronoUnit.MINUTES.between(now, ticket.getSlaResolveDeadline()));
            }
        }
        return s;
    }
}
