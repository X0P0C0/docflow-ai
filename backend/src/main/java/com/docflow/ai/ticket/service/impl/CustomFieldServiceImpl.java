package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.ticket.dto.CreateCustomFieldRequest;
import com.docflow.ai.ticket.dto.CustomFieldResponse;
import com.docflow.ai.ticket.dto.CustomFieldValueRequest;
import com.docflow.ai.ticket.entity.CustomField;
import com.docflow.ai.ticket.entity.CustomFieldValue;
import com.docflow.ai.ticket.mapper.CustomFieldMapper;
import com.docflow.ai.ticket.mapper.CustomFieldValueMapper;
import com.docflow.ai.ticket.service.CustomFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomFieldServiceImpl implements CustomFieldService {

    private final CustomFieldMapper fieldMapper;
    private final CustomFieldValueMapper fieldValueMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<CustomFieldResponse> listFields(Long userId, Long categoryId) {
        userAccessService.requireActiveUser(userId);
        LambdaQueryWrapper<CustomField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomField::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(CustomField::getCategoryId, categoryId);
        }
        wrapper.orderByAsc(CustomField::getSortOrder);
        return fieldMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomFieldResponse createField(Long userId, CreateCustomFieldRequest request) {
        userAccessService.requireTicketOperator(userId);
        CustomField field = new CustomField();
        field.setFieldName(request.getFieldName());
        field.setFieldLabel(request.getFieldLabel());
        field.setFieldType(request.getFieldType());
        field.setOptions(request.getOptions());
        field.setDefaultValue(request.getDefaultValue());
        field.setRequired(request.getRequired() != null ? request.getRequired() : 0);
        field.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        field.setCategoryId(request.getCategoryId());
        field.setStatus(1);
        fieldMapper.insert(field);
        return toResponse(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomFieldResponse updateField(Long id, Long userId, CreateCustomFieldRequest request) {
        userAccessService.requireTicketOperator(userId);
        CustomField field = fieldMapper.selectById(id);
        if (field == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Custom field not found");
        field.setFieldName(request.getFieldName());
        field.setFieldLabel(request.getFieldLabel());
        field.setFieldType(request.getFieldType());
        field.setOptions(request.getOptions());
        field.setDefaultValue(request.getDefaultValue());
        if (request.getRequired() != null) field.setRequired(request.getRequired());
        if (request.getSortOrder() != null) field.setSortOrder(request.getSortOrder());
        if (request.getCategoryId() != null) field.setCategoryId(request.getCategoryId());
        fieldMapper.updateById(field);
        return toResponse(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        CustomField field = fieldMapper.selectById(id);
        if (field == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND, "Custom field not found");
        field.setStatus(0);
        fieldMapper.updateById(field);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFieldValues(Long ticketId, Long userId, List<CustomFieldValueRequest> values) {
        if (values == null || values.isEmpty()) return;
        for (CustomFieldValueRequest req : values) {
            LambdaQueryWrapper<CustomFieldValue> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomFieldValue::getTicketId, ticketId);
            wrapper.eq(CustomFieldValue::getFieldId, req.getFieldId());
            CustomFieldValue existing = fieldValueMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setFieldValue(req.getFieldValue());
                existing.setUpdateBy(userId);
                fieldValueMapper.updateById(existing);
            } else {
                CustomFieldValue val = new CustomFieldValue();
                val.setTicketId(ticketId);
                val.setFieldId(req.getFieldId());
                val.setFieldValue(req.getFieldValue());
                val.setCreateBy(userId);
                fieldValueMapper.insert(val);
            }
        }
    }

    @Override
    public Map<Long, String> getFieldValues(Long ticketId) {
        LambdaQueryWrapper<CustomFieldValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFieldValue::getTicketId, ticketId);
        Map<Long, String> result = new HashMap<>();
        fieldValueMapper.selectList(wrapper).forEach(v -> result.put(v.getFieldId(), v.getFieldValue()));
        return result;
    }

    private CustomFieldResponse toResponse(CustomField field) {
        CustomFieldResponse r = new CustomFieldResponse();
        r.setId(field.getId());
        r.setFieldName(field.getFieldName());
        r.setFieldLabel(field.getFieldLabel());
        r.setFieldType(field.getFieldType());
        r.setOptions(field.getOptions());
        r.setDefaultValue(field.getDefaultValue());
        r.setRequired(field.getRequired());
        r.setSortOrder(field.getSortOrder());
        r.setCategoryId(field.getCategoryId());
        r.setStatus(field.getStatus());
        r.setCreateTime(field.getCreateTime());
        return r;
    }
}
