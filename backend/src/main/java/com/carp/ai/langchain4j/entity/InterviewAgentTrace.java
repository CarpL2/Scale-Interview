package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_agent_trace")
public class InterviewAgentTrace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private Integer turnIndex;

    private String currentStage;

    private String plannerInstruction;

    private String userMessage;

    private String agentResponse;

    private String toolStatus;

    private String toolName;

    private String toolQuery;

    private Boolean toolFallbackUsed;

    private String toolResultSummary;

    private LocalDateTime createTime;
}
