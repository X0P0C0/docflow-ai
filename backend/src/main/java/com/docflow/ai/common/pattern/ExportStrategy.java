package com.docflow.ai.common.pattern;

import com.docflow.ai.ticket.entity.Ticket;
import java.util.List;
import java.util.Map;

public interface ExportStrategy {
    String getFormat();
    String export(List<Ticket> tickets, Map<Long, String> userNameMap);
}
