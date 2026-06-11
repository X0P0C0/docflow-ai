package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.ticket.dto.TicketReportResponse;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.service.ReportService;
import com.docflow.ai.ticket.service.SatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;
    private final SatisfactionService satisfactionService;

    @Override
    public TicketReportResponse generateDailyReport() {
        LocalDate today = LocalDate.now();
        return buildReport(today.atStartOfDay(), today.atTime(LocalTime.MAX), "DAILY", today.toString());
    }

    @Override
    public TicketReportResponse generateWeeklyReport() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        String period = weekStart.format(DateTimeFormatter.ofPattern("MM-dd")) + " ~ " + today.format(DateTimeFormatter.ofPattern("MM-dd"));
        return buildReport(weekStart.atStartOfDay(), today.atTime(LocalTime.MAX), "WEEKLY", period);
    }

    private TicketReportResponse buildReport(LocalDateTime start, LocalDateTime end, String type, String period) {
        TicketReportResponse report = new TicketReportResponse();
        report.setReportType(type);
        report.setPeriod(period);

        // Created tickets
        LambdaQueryWrapper<Ticket> created = new LambdaQueryWrapper<>();
        created.ge(Ticket::getCreateTime, start).le(Ticket::getCreateTime, end).eq(Ticket::getDeleted, 0);
        report.setCreatedCount(ticketMapper.selectCount(created));

        // Resolved tickets
        LambdaQueryWrapper<Ticket> resolved = new LambdaQueryWrapper<>();
        resolved.ge(Ticket::getResolvedTime, start).le(Ticket::getResolvedTime, end).eq(Ticket::getDeleted, 0);
        report.setResolvedCount(ticketMapper.selectCount(resolved));

        // Closed tickets
        LambdaQueryWrapper<Ticket> closed = new LambdaQueryWrapper<>();
        closed.ge(Ticket::getCloseTime, start).le(Ticket::getCloseTime, end).eq(Ticket::getDeleted, 0);
        report.setClosedCount(ticketMapper.selectCount(closed));

        // SLA breached (tickets that exceeded SLA deadlines)
        LambdaQueryWrapper<Ticket> breached = new LambdaQueryWrapper<>();
        breached.lt(Ticket::getSlaResolveDeadline, LocalDateTime.now())
                .in(Ticket::getStatus, 1, 2)
                .eq(Ticket::getDeleted, 0);
        report.setBreachedCount(ticketMapper.selectCount(breached));

        // Average resolution time
        LambdaQueryWrapper<Ticket> resolvedTickets = new LambdaQueryWrapper<>();
        resolvedTickets.isNotNull(Ticket::getResolvedTime).eq(Ticket::getDeleted, 0);
        List<Ticket> allResolved = ticketMapper.selectList(resolvedTickets);
        double avgHours = allResolved.stream()
                .filter(t -> t.getCreateTime() != null && t.getResolvedTime() != null)
                .mapToLong(t -> ChronoUnit.MINUTES.between(t.getCreateTime(), t.getResolvedTime()))
                .average()
                .orElse(0);
        report.setAvgResolutionHours(Math.round(avgHours / 60.0 * 10) / 10.0);

        // Top assignees by resolved count
        Map<Long, Long> assigneeCounts = allResolved.stream()
                .filter(t -> t.getAssigneeUserId() != null)
                .collect(Collectors.groupingBy(Ticket::getAssigneeUserId, Collectors.counting()));
        List<TicketReportResponse.TopAssignee> topAssignees = assigneeCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    TicketReportResponse.TopAssignee a = new TicketReportResponse.TopAssignee();
                    a.setUserId(e.getKey());
                    var user = sysUserMapper.selectById(e.getKey());
                    a.setName(user != null ? user.getRealName() : "Unknown");
                    a.setResolvedCount(e.getValue());
                    double avg = allResolved.stream()
                            .filter(t -> e.getKey().equals(t.getAssigneeUserId()) && t.getCreateTime() != null && t.getResolvedTime() != null)
                            .mapToLong(t -> ChronoUnit.MINUTES.between(t.getCreateTime(), t.getResolvedTime()))
                            .average().orElse(0);
                    a.setAvgResolutionHours(Math.round(avg / 60.0 * 10) / 10.0);
                    return a;
                })
                .toList();
        report.setTopAssignees(topAssignees);

        // Category breakdown
        LambdaQueryWrapper<Ticket> allTickets = new LambdaQueryWrapper<>();
        allTickets.ge(Ticket::getCreateTime, start).le(Ticket::getCreateTime, end).eq(Ticket::getDeleted, 0);
        Map<String, Long> typeCounts = ticketMapper.selectList(allTickets).stream()
                .collect(Collectors.groupingBy(t -> t.getType() != null ? t.getType() : "UNKNOWN", Collectors.counting()));
        report.setCategoryBreakdown(typeCounts.entrySet().stream()
                .map(e -> {
                    TicketReportResponse.CategoryBreakdown cb = new TicketReportResponse.CategoryBreakdown();
                    cb.setCategory(e.getKey());
                    cb.setCount(e.getValue());
                    return cb;
                })
                .toList());

        // Generate summary text
        report.setSummary(generateSummary(report));

        return report;
    }

    private String generateSummary(TicketReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s report: ", report.getReportType().equals("DAILY") ? "Daily" : "Weekly"));
        sb.append(String.format("%d tickets created, %d resolved, %d closed. ",
                report.getCreatedCount(), report.getResolvedCount(), report.getClosedCount()));
        if (report.getAvgResolutionHours() > 0) {
            sb.append(String.format("Average resolution time: %.1f hours. ", report.getAvgResolutionHours()));
        }
        if (report.getBreachedCount() > 0) {
            sb.append(String.format("WARNING: %d tickets breached SLA. ", report.getBreachedCount()));
        }
        if (report.getTopAssignees() != null && !report.getTopAssignees().isEmpty()) {
            sb.append(String.format("Top resolver: %s (%d tickets). ",
                    report.getTopAssignees().get(0).getName(),
                    report.getTopAssignees().get(0).getResolvedCount()));
        }
        return sb.toString();
    }
}
