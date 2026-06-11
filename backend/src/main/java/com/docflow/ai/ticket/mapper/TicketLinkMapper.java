package com.docflow.ai.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docflow.ai.ticket.entity.TicketLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketLinkMapper extends BaseMapper<TicketLink> {

    @Select("SELECT * FROM ticket_link WHERE ticket_id = #{ticketId} OR linked_ticket_id = #{ticketId}")
    List<TicketLink> findAllByTicketId(@Param("ticketId") Long ticketId);
}