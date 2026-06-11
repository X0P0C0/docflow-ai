package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.dto.CategoryResponse;
import com.docflow.ai.ticket.dto.CreateCategoryRequest;
import com.docflow.ai.ticket.entity.TicketCategory;
import com.docflow.ai.ticket.mapper.TicketCategoryMapper;
import com.docflow.ai.ticket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final TicketCategoryMapper categoryMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<CategoryResponse> listCategories(Long userId) {
        userAccessService.requireActiveUser(userId);
        List<TicketCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategory>()
                        .eq(TicketCategory::getStatus, 1)
                        .eq(TicketCategory::getDeleted, 0)
                        .orderByAsc(TicketCategory::getSortOrder));
        Map<Long, List<TicketCategory>> byParent = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(TicketCategory::getParentId));
        return all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .map(root -> toTree(root, byParent))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketCategory cat = new TicketCategory();
        cat.setName(request.getName());
        cat.setParentId(request.getParentId() != null ? request.getParentId() : 0);
        cat.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        cat.setStatus(1);
        cat.setDeleted(0);
        cat.setCreateBy(userId);
        categoryMapper.insert(cat);
        return toResponse(cat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryResponse updateCategory(Long id, Long userId, CreateCategoryRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketCategory cat = categoryMapper.selectById(id);
        if (cat == null || Integer.valueOf(1).equals(cat.getDeleted())) {
            throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Category not found");
        }
        cat.setName(request.getName());
        if (request.getParentId() != null) cat.setParentId(request.getParentId() != null ? request.getParentId() : 0);
        if (request.getSortOrder() != null) cat.setSortOrder(request.getSortOrder());
        cat.setUpdateBy(userId);
        categoryMapper.updateById(cat);
        return toResponse(cat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        TicketCategory cat = categoryMapper.selectById(id);
        if (cat == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        cat.setDeleted(1);
        cat.setUpdateBy(userId);
        categoryMapper.updateById(cat);
    }

    private CategoryResponse toTree(TicketCategory root, Map<Long, List<TicketCategory>> byParent) {
        CategoryResponse r = toResponse(root);
        List<TicketCategory> children = byParent.getOrDefault(root.getId(), List.of());
        r.setChildren(children.stream().map(c -> toTree(c, byParent)).toList());
        return r;
    }

    private CategoryResponse toResponse(TicketCategory c) {
        CategoryResponse r = new CategoryResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setParentId(c.getParentId());
        r.setSortOrder(c.getSortOrder());
        r.setStatus(c.getStatus());
        r.setCreateTime(c.getCreateTime());
        return r;
    }
}
