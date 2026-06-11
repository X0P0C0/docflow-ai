package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.dto.CreateTemplateRequest;
import com.docflow.ai.ticket.dto.TemplateResponse;
import com.docflow.ai.ticket.entity.TicketTemplate;
import com.docflow.ai.ticket.mapper.TicketTemplateMapper;
import com.docflow.ai.ticket.service.TicketTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketTemplateServiceImpl implements TicketTemplateService {

    private final TicketTemplateMapper templateMapper;
    private final SysUserMapper sysUserMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<TemplateResponse> listTemplates(Long userId) {
        userAccessService.requireActiveUser(userId);
        LambdaQueryWrapper<TicketTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketTemplate::getDeleted, 0);
        wrapper.orderByDesc(TicketTemplate::getCreateTime);
        return templateMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    @Override
    public TemplateResponse getTemplate(Long id) {
        TicketTemplate tpl = templateMapper.selectById(id);
        if (tpl == null || Integer.valueOf(1).equals(tpl.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Template not found");
        }
        return toResponse(tpl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponse createTemplate(Long userId, CreateTemplateRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketTemplate tpl = new TicketTemplate();
        tpl.setName(request.getName());
        tpl.setTitleTemplate(request.getTitleTemplate());
        tpl.setContentTemplate(request.getContentTemplate());
        tpl.setType(request.getType());
        tpl.setPriority(request.getPriority());
        tpl.setCategoryId(request.getCategoryId());
        tpl.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 1);
        tpl.setCreateBy(userId);
        tpl.setDeleted(0);
        templateMapper.insert(tpl);
        return toResponse(tpl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateResponse updateTemplate(Long id, Long userId, CreateTemplateRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketTemplate tpl = templateMapper.selectById(id);
        if (tpl == null || Integer.valueOf(1).equals(tpl.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Template not found");
        }
        tpl.setName(request.getName());
        tpl.setTitleTemplate(request.getTitleTemplate());
        tpl.setContentTemplate(request.getContentTemplate());
        tpl.setType(request.getType());
        tpl.setPriority(request.getPriority());
        tpl.setCategoryId(request.getCategoryId());
        if (request.getIsPublic() != null) tpl.setIsPublic(request.getIsPublic());
        tpl.setUpdateBy(userId);
        templateMapper.updateById(tpl);
        return toResponse(tpl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        TicketTemplate tpl = templateMapper.selectById(id);
        if (tpl == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Template not found");
        tpl.setDeleted(1);
        tpl.setUpdateBy(userId);
        templateMapper.updateById(tpl);
    }

    private TemplateResponse toResponse(TicketTemplate tpl) {
        TemplateResponse r = new TemplateResponse();
        r.setId(tpl.getId());
        r.setName(tpl.getName());
        r.setTitleTemplate(tpl.getTitleTemplate());
        r.setContentTemplate(tpl.getContentTemplate());
        r.setType(tpl.getType());
        r.setPriority(tpl.getPriority());
        r.setCategoryId(tpl.getCategoryId());
        r.setIsPublic(tpl.getIsPublic());
        r.setCreateBy(tpl.getCreateBy());
        r.setCreateTime(tpl.getCreateTime());
        if (tpl.getCreateBy() != null) {
            var user = sysUserMapper.selectById(tpl.getCreateBy());
            if (user != null) r.setCreatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        return r;
    }
}
