package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_session_state")
public class InterviewSessionState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private String currentStage;
    private Integer questionCount;
    private String sessionSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
