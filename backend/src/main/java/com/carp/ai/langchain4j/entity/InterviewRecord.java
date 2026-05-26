package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史面试记录表（每场面试生成一条记录）
 */
@Data
@TableName("interview_record")
public class InterviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID（关联 MongoDB 的记忆）
     */
    private String sessionId;

    /**
     * 候选人本次投递用的简历
     */
    private String resumeContent;

    /**
     * 本次面试的岗位JD
     */
    private String jdContent;

    /**
     * 本次面试的风格：PROFESSIONAL-专业, STERN-严厉, ENCOURAGING-鼓励
     */
    private String interviewStyle;

    /**
     * 成绩单 JSON
     */
    private String evaluationReport;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
