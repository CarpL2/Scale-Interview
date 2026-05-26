package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户面试画像实体类
 * 用于存储用户的简历和JD信息，实现面试逻辑的隔离和持久化
 */
@Data
@TableName("user_interview_profile")
public class UserInterviewProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户唯一标识（隔离核心）
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户的弱点画像（由 AI 辅导后动态生成）
     */
    private String weaknessProfile;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
