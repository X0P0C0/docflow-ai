package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.ticket.dto.SatisfactionStatsResponse;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.service.SatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SatisfactionServiceImpl implements SatisfactionService {

    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;
    private final UserAccessService userAccessService;

    @Override
    public SatisfactionStatsResponse getStats(Long userId) {
        userAccessService.requireActiveUser(userId);
        SatisfactionStatsResponse stats = new SatisfactionStatsResponse();

        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Ticket::getSatisfactionScore);
        List<Ticket> rated = ticketMapper.selectList(wrapper);

        stats.setTotalRated(rated.size());
        if (rated.isEmpty()) {
            stats.setAverageScore(0);
            stats.setDistribution(new HashMap<>());
            stats.setByAssignee(new ArrayList<>());
            return stats;
        }

        double avg = rated.stream().mapToInt(Ticket::getSatisfactionScore).average().orElse(0);
        stats.setAverageScore(Math.round(avg * 100.0) / 100.0);

        Map<Integer, Long> dist = rated.stream()
                .collect(Collectors.groupingBy(Ticket::getSatisfactionScore, Collectors.counting()));
        stats.setDistribution(dist);

        Map<Long, List<Ticket>> byAssignee = rated.stream()
                .filter(t -> t.getAssigneeUserId() != null)
                .collect(Collectors.groupingBy(Ticket::getAssigneeUserId));
        List<SatisfactionStatsResponse.AssigneeSatisfaction> assigneeStats = new ArrayList<>();
        for (Map.Entry<Long, List<Ticket>> entry : byAssignee.entrySet()) {
            SatisfactionStatsResponse.AssigneeSatisfaction as = new SatisfactionStatsResponse.AssigneeSatisfaction();
            as.setAssigneeId(entry.getKey());
            var user = sysUserMapper.selectById(entry.getKey());
            as.setAssigneeName(user != null ? (user.getRealName() != null ? user.getRealName() : user.getUsername()) : "Unknown");
            as.setRatedCount(entry.getValue().size());
            as.setAvgScore(Math.round(entry.getValue().stream().mapToInt(Ticket::getSatisfactionScore).average().orElse(0) * 100.0) / 100.0);
            assigneeStats.add(as);
        }
        assigneeStats.sort((a, b) -> Double.compare(b.getAvgScore(), a.getAvgScore()));
        stats.setByAssignee(assigneeStats);

        return stats;
    }
}
