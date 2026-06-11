package com.docflow.ai.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConversationSummary {
    private String summary;
    private List<String> keyPoints;
    private String rootCause;
    private String resolution;
    private List<String> actionItems;
    private String sentiment;
}
