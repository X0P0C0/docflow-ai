package com.docflow.ai.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AutoReplyResult {
    private String replyContent;
    private String tone;
    private double confidence;
    private List<String> sources;
    private String disclaimer;
}
