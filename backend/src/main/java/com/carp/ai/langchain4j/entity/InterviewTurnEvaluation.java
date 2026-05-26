package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_turn_evaluation")
public class InterviewTurnEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private Integer turnIndex;
    private String question;
    private String userAnswer;
    private Integer technicalScore;
    private Integer communicationScore;
    private String feedback;
    private String extractedWeaknesses;
    private LocalDateTime createTime;
}
