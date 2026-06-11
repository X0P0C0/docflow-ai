package com.docflow.ai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docflow.ai.system.entity.WebhookLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WebhookLogMapper extends BaseMapper<WebhookLog> {
}
