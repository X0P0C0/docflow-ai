package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.dto.CreateTagRequest;
import com.docflow.ai.ticket.dto.TagResponse;
import com.docflow.ai.ticket.entity.TicketTag;
import com.docflow.ai.ticket.entity.TicketTagRelation;
import com.docflow.ai.ticket.mapper.TicketTagMapper;
import com.docflow.ai.ticket.mapper.TicketTagRelationMapper;
import com.docflow.ai.ticket.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TicketTagMapper tagMapper;
    private final TicketTagRelationMapper relationMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<TagResponse> listTags(Long userId) {
        userAccessService.requireActiveUser(userId);
        List<TicketTag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<TicketTag>().eq(TicketTag::getDeleted, 0).orderByAsc(TicketTag::getId));
        List<TagResponse> result = new ArrayList<>();
        for (TicketTag tag : tags) {
            TagResponse r = toResponse(tag);
            LambdaQueryWrapper<TicketTagRelation> countW = new LambdaQueryWrapper<>();
            countW.eq(TicketTagRelation::getTagId, tag.getId());
            r.setUsageCount(relationMapper.selectCount(countW));
            result.add(r);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagResponse createTag(Long userId, CreateTagRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketTag tag = new TicketTag();
        tag.setName(request.getName());
        tag.setColor(request.getColor() != null ? request.getColor() : "#409EFF");
        tag.setDeleted(0);
        tag.setCreateBy(userId);
        tagMapper.insert(tag);
        return toResponse(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        TicketTag tag = tagMapper.selectById(id);
        if (tag == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        tag.setDeleted(1);
        tagMapper.updateById(tag);
    }

    @Override
    public void addTagToTicket(Long ticketId, Long tagId) {
        TicketTagRelation rel = new TicketTagRelation();
        rel.setTicketId(ticketId);
        rel.setTagId(tagId);
        rel.setCreateTime(LocalDateTime.now());
        try { relationMapper.insert(rel); } catch (Exception ignored) {}
    }

    @Override
    public void removeTagFromTicket(Long ticketId, Long tagId) {
        LambdaQueryWrapper<TicketTagRelation> w = new LambdaQueryWrapper<>();
        w.eq(TicketTagRelation::getTicketId, ticketId).eq(TicketTagRelation::getTagId, tagId);
        relationMapper.delete(w);
    }

    @Override
    public List<TagResponse> getTicketTags(Long ticketId) {
        LambdaQueryWrapper<TicketTagRelation> w = new LambdaQueryWrapper<>();
        w.eq(TicketTagRelation::getTicketId, ticketId);
        List<TicketTagRelation> rels = relationMapper.selectList(w);
        List<TagResponse> result = new ArrayList<>();
        for (TicketTagRelation rel : rels) {
            TicketTag tag = tagMapper.selectById(rel.getTagId());
            if (tag != null && Integer.valueOf(0).equals(tag.getDeleted())) {
                result.add(toResponse(tag));
            }
        }
        return result;
    }

    private TagResponse toResponse(TicketTag tag) {
        TagResponse r = new TagResponse();
        r.setId(tag.getId());
        r.setName(tag.getName());
        r.setColor(tag.getColor());
        r.setCreateTime(tag.getCreateTime());
        return r;
    }
}
