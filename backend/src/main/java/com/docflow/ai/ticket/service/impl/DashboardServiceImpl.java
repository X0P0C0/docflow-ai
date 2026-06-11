package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.ticket.dto.*;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import com.docflow.ai.ticket.service.DashboardService;
import com.docflow.ai.ticket.service.SatisfactionService;
import com.docflow.ai.ticket.service.SlaService;
import com.docflow.ai.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TicketService ticketService;
    private final SatisfactionService satisfactionService;
    private final SlaService slaService;
    private final TicketMapper ticketMapper;
    private final TicketRecordMapper recordMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public DashboardResponse getDashboard(Long userId) {
        DashboardResponse dashboard = new DashboardResponse();

        dashboard.setTicketStats(ticketService.getTicketStats(userId));
        dashboard.setSatisfactionStats(satisfactionService.getStats(userId));

        // Status trend (last 7 days)
        List<DashboardResponse.StatusTrend> trends = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            DashboardResponse.StatusTrend trend = new DashboardResponse.StatusTrend();
            trend.setDate(date.format(fmt));
            LambdaQueryWrapper<Ticket> created = new LambdaQueryWrapper<>();
            created.ge(Ticket::getCreateTime, dayStart).le(Ticket::getCreateTime, dayEnd);
            trend.setCreated(ticketMapper.selectCount(created));
            LambdaQueryWrapper<Ticket> resolved = new LambdaQueryWrapper<>();
            resolved.ge(Ticket::getResolvedTime, dayStart).le(Ticket::getResolvedTime, dayEnd);
            trend.setResolved(ticketMapper.selectCount(resolved));
            LambdaQueryWrapper<Ticket> closed = new LambdaQueryWrapper<>();
            closed.ge(Ticket::getCloseTime, dayStart).le(Ticket::getCloseTime, dayEnd);
            trend.setClosed(ticketMapper.selectCount(closed));
            trends.add(trend);
        }
        dashboard.setStatusTrend(trends);

        // Type distribution
        List<DashboardResponse.TypeDistribution> types = new ArrayList<>();
        for (String type : List.of("INCIDENT", "TASK", "QUESTION")) {
            LambdaQueryWrapper<Ticket> w = new LambdaQueryWrapper<>();
            w.eq(Ticket::getType, type);
            long count = ticketMapper.selectCount(w);
            if (count > 0) {
                DashboardResponse.TypeDistribution td = new DashboardResponse.TypeDistribution();
                td.setType(type);
                td.setCount(count);
                types.add(td);
            }
        }
        dashboard.setTypeDistribution(types);

        // Priority distribution
        List<DashboardResponse.PriorityDistribution> priorities = new ArrayList<>();
        String[] labels = {"", "Critical", "High", "Medium", "Low"};
        for (int p = 1; p <= 4; p++) {
            LambdaQueryWrapper<Ticket> w = new LambdaQueryWrapper<>();
            w.eq(Ticket::getPriority, p);
            long count = ticketMapper.selectCount(w);
            DashboardResponse.PriorityDistribution pd = new DashboardResponse.PriorityDistribution();
            pd.setPriority(p);
            pd.setLabel(labels[p]);
            pd.setCount(count);
            priorities.add(pd);
        }
        dashboard.setPriorityDistribution(priorities);

        // Recent activities
        LambdaQueryWrapper<TicketRecord> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.orderByDesc(TicketRecord::getCreateTime);
        recentWrapper.last("LIMIT 10");
        List<TicketRecord> recent = recordMapper.selectList(recentWrapper);
        List<DashboardResponse.RecentActivity> activities = new ArrayList<>();
        for (TicketRecord r : recent) {
            DashboardResponse.RecentActivity a = new DashboardResponse.RecentActivity();
            a.setTicketId(r.getTicketId());
            Ticket t = ticketMapper.selectById(r.getTicketId());
            if (t != null) { a.setTicketNo(t.getTicketNo()); a.setTitle(t.getTitle()); }
            a.setActionType(r.getActionType());
            a.setRemark(r.getRemark());
            a.setTime(r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            if (r.getOperatorUserId() != null) {
                var user = sysUserMapper.selectById(r.getOperatorUserId());
                a.setOperatorName(user != null ? (user.getRealName() != null ? user.getRealName() : user.getUsername()) : "");
            }
            activities.add(a);
        }
        dashboard.setRecentActivities(activities);

        // Breached + unassigned counts
        try { dashboard.setBreachedCount(slaService.getBreachedTickets(userId).size()); } catch (Exception e) { dashboard.setBreachedCount(0); }
        LambdaQueryWrapper<Ticket> unassigned = new LambdaQueryWrapper<>();
        unassigned.isNull(Ticket::getAssigneeUserId).in(Ticket::getStatus, List.of(1, 2));
        dashboard.setUnassignedCount(ticketMapper.selectCount(unassigned));

        return dashboard;
    }
}
