package com.docflow.ai.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.common.domain.PageResponse;
import com.docflow.ai.customer.dto.*;
import com.docflow.ai.customer.entity.CustomerUser;
import com.docflow.ai.customer.mapper.CustomerUserMapper;
import com.docflow.ai.customer.auth.CustomerTokenStore;
import com.docflow.ai.customer.service.CustomerPortalService;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class CustomerPortalServiceImpl implements CustomerPortalService {

    private final CustomerUserMapper customerMapper;
    private final TicketMapper ticketMapper;
    private final TicketRecordMapper recordMapper;
    private final SysUserMapper sysUserMapper;
    private final CustomerTokenStore tokenStore;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final AtomicLong TICKET_SEQ = new AtomicLong(System.currentTimeMillis() % 10000);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerLoginResponse register(CustomerRegisterRequest request) {
        LambdaQueryWrapper<CustomerUser> w = new LambdaQueryWrapper<>();
        w.eq(CustomerUser::getUsername, request.getUsername());
        if (customerMapper.selectOne(w) != null) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "Username already exists");
        }
        CustomerUser user = new CustomerUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCompany(request.getCompany());
        user.setRealName(request.getRealName());
        user.setStatus(1);
        user.setDeleted(0);
        customerMapper.insert(user);
        return buildLoginResponse(user);
    }

    @Override
    public CustomerLoginResponse login(CustomerLoginRequest request) {
        LambdaQueryWrapper<CustomerUser> w = new LambdaQueryWrapper<>();
        w.eq(CustomerUser::getUsername, request.getUsername()).eq(CustomerUser::getDeleted, 0);
        CustomerUser user = customerMapper.selectOne(w);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return buildLoginResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerTicketResponse submitTicket(Long customerId, CustomerTicketRequest request) {
        CustomerUser customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BusinessException(ResultCode.LOGIN_FAILED);

        String type = request.getType() != null ? request.getType() : "INCIDENT";
        int priority = request.getPriority() != null ? request.getPriority() : 3;
        String ticketNo = generateTicketNo(type);

        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNo);
        ticket.setTitle(request.getTitle().trim());
        ticket.setContent(request.getContent().trim());
        ticket.setType(type);
        ticket.setPriority(priority);
        ticket.setStatus(1);
        ticket.setSource("CUSTOMER_PORTAL");
        ticket.setSubmitUserId(1L); // Default to system admin as submit user for customer tickets
        ticket.setDeleted(0);
        ticketMapper.insert(ticket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorUserId(1L);
        record.setActionType("CREATE");
        record.setNewStatus(1);
        record.setRemark("Customer " + customer.getUsername() + " submitted ticket");
        recordMapper.insert(record);

        return buildTicketResponse(ticket, customer);
    }

    @Override
    public List<CustomerTicketResponse> listMyTickets(Long customerId) {
        // For customer portal, we use submitUserId to link tickets
        // In a real system, we would have a customer_ticket relation
        LambdaQueryWrapper<Ticket> w = new LambdaQueryWrapper<>();
        w.eq(Ticket::getDeleted, 0).eq(Ticket::getSource, "CUSTOMER_PORTAL");
        w.orderByDesc(Ticket::getCreateTime);
        w.last("LIMIT 50");
        return ticketMapper.selectList(w).stream()
                .map(t -> buildTicketResponse(t, null))
                .toList();
    }

    @Override
    public CustomerTicketResponse getTicketDetail(Long ticketId, Long customerId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        }
        CustomerTicketResponse resp = buildTicketResponse(ticket, null);

        // Build timeline
        LambdaQueryWrapper<TicketRecord> rw = new LambdaQueryWrapper<>();
        rw.eq(TicketRecord::getTicketId, ticketId).orderByAsc(TicketRecord::getCreateTime);
        List<CustomerTicketResponse.TimelineItem> timeline = new ArrayList<>();
        for (TicketRecord r : recordMapper.selectList(rw)) {
            CustomerTicketResponse.TimelineItem item = new CustomerTicketResponse.TimelineItem();
            item.setActionType(r.getActionType());
            item.setRemark(r.getRemark());
            item.setTime(r.getCreateTime() != null ? r.getCreateTime().toString() : "");
            if (r.getOperatorUserId() != null) {
                var user = sysUserMapper.selectById(r.getOperatorUserId());
                item.setOperatorName(user != null ? user.getRealName() : "System");
            }
            timeline.add(item);
        }
        resp.setTimeline(timeline);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerTicketResponse rateSatisfaction(Long ticketId, Long customerId, CustomerSatisfactionRequest request) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        if (ticket.getStatus() < 3) {
            throw new BusinessException(ResultCode.BUSINESS_RULE_VIOLATION, "Can only rate resolved tickets");
        }
        if (ticket.getSatisfactionScore() != null) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "Already rated");
        }
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setSatisfactionScore(request.getScore());
        update.setSatisfactionComment(request.getComment());
        ticketMapper.updateById(update);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticketId);
        record.setActionType("SATISFACTION");
        record.setOldStatus(ticket.getStatus());
        record.setNewStatus(ticket.getStatus());
        record.setRemark("Customer rated: " + request.getScore() + "/5");
        recordMapper.insert(record);

        ticket.setSatisfactionScore(request.getScore());
        ticket.setSatisfactionComment(request.getComment());
        return buildTicketResponse(ticket, null);
    }

    private CustomerLoginResponse buildLoginResponse(CustomerUser user) {
        CustomerLoginResponse resp = new CustomerLoginResponse();
        resp.setToken(tokenStore.createToken(user.getId()));
        resp.setExpireSeconds(86400);
        CustomerLoginResponse.CustomerInfo info = new CustomerLoginResponse.CustomerInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setCompany(user.getCompany());
        info.setRealName(user.getRealName());
        resp.setCustomer(info);
        return resp;
    }

    private CustomerTicketResponse buildTicketResponse(Ticket ticket, CustomerUser customer) {
        CustomerTicketResponse r = new CustomerTicketResponse();
        r.setId(ticket.getId());
        r.setTicketNo(ticket.getTicketNo());
        r.setTitle(ticket.getTitle());
        r.setContent(ticket.getContent());
        r.setType(ticket.getType());
        r.setPriority(ticket.getPriority());
        r.setPriorityLabel(mapPriority(ticket.getPriority()));
        r.setStatus(ticket.getStatus());
        r.setStatusLabel(mapStatus(ticket.getStatus()));
        r.setSatisfactionScore(ticket.getSatisfactionScore());
        r.setCreateTime(ticket.getCreateTime());
        r.setUpdateTime(ticket.getUpdateTime());
        if (ticket.getAssigneeUserId() != null) {
            var user = sysUserMapper.selectById(ticket.getAssigneeUserId());
            r.setAssigneeName(user != null ? user.getRealName() : null);
        }
        return r;
    }

    private String generateTicketNo(String type) {
        String prefix = switch (type.toUpperCase()) {
            case "TASK" -> "TSK";
            case "QUESTION" -> "QST";
            default -> "INC";
        };
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + "-" + date + "-" + String.format("%05d", TICKET_SEQ.incrementAndGet());
    }

    private String mapPriority(int p) {
        return switch (p) { case 1 -> "Critical"; case 2 -> "High"; case 3 -> "Medium"; case 4 -> "Low"; default -> "Unknown"; };
    }

    private String mapStatus(int s) {
        return switch (s) { case 1 -> "Open"; case 2 -> "In Progress"; case 3 -> "Resolved"; case 4 -> "Closed"; default -> "Unknown"; };
    }
}
