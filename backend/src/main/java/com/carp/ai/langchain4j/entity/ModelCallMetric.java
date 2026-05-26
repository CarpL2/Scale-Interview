package com.carp.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_call_metric")
public class ModelCallMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private String agentName;

    private String modelName;

    private Boolean streaming;

    private Boolean success;

    private Long costMs;

    private String errorType;

    private String errorMessage;

    private LocalDateTime createTime;
}
