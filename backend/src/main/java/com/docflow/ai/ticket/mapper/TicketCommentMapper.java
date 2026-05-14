package com.docflow.ai.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docflow.ai.ticket.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketComment> {
}
