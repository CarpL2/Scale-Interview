package com.carp.ai.langchain4j.bean;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentDecisionSummary {
    private Integer turnIndex;
    private String currentStage;
    private String decisionSummary;
    private String toolStatus;
    private LocalDateTime createTime;
}
