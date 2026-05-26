package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.ModelCallMetric;
import com.carp.ai.langchain4j.mapper.ModelCallMetricMapper;
import com.carp.ai.langchain4j.service.ModelCallMetricService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModelCallMetricServiceImpl
        extends ServiceImpl<ModelCallMetricMapper, ModelCallMetric>
        implements ModelCallMetricService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureMetricTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS model_call_metric (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT COMMENT '用户ID',
                    session_id VARCHAR(128) COMMENT '会话ID',
                    agent_name VARCHAR(64) NOT NULL COMMENT 'Agent名称',
                    model_name VARCHAR(64) COMMENT '模型名称',
                    streaming TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否流式调用',
                    success TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
                    cost_ms BIGINT COMMENT '耗时毫秒',
                    error_type VARCHAR(128) COMMENT '错误类型',
                    error_message VARCHAR(500) COMMENT '错误摘要',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    INDEX idx_user_time (user_id, create_time),
                    INDEX idx_session_time (session_id, create_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型调用指标表'
                """);
    }

    @Override
    public long start() {
        return System.nanoTime();
    }

    @Override
    public void recordSuccess(Long userId, String sessionId, String agentName, String modelName, boolean streaming, long startTime) {
        saveMetric(userId, sessionId, agentName, modelName, streaming, true, startTime, null);
    }

    @Override
    public void recordFailure(Long userId,
                              String sessionId,
                              String agentName,
                              String modelName,
                              boolean streaming,
                              long startTime,
                              Throwable error) {
        saveMetric(userId, sessionId, agentName, modelName, streaming, false, startTime, error);
    }

    @Override
    public List<ModelCallMetric> getRecentMetrics(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return lambdaQuery()
                .eq(ModelCallMetric::getUserId, userId)
                .orderByDesc(ModelCallMetric::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();
    }

    private void saveMetric(Long userId,
                            String sessionId,
                            String agentName,
                            String modelName,
                            boolean streaming,
                            boolean success,
                            long startTime,
                            Throwable error) {
        try {
            ModelCallMetric metric = new ModelCallMetric();
            metric.setUserId(userId);
            metric.setSessionId(sessionId);
            metric.setAgentName(agentName);
            metric.setModelName(modelName);
            metric.setStreaming(streaming);
            metric.setSuccess(success);
            metric.setCostMs((System.nanoTime() - startTime) / 1_000_000);
            if (error != null) {
                metric.setErrorType(error.getClass().getSimpleName());
                metric.setErrorMessage(truncate(error.getMessage(), 500));
            }
            metric.setCreateTime(LocalDateTime.now());
            save(metric);
        } catch (Exception ignored) {
            // 指标写入不能影响主业务链路。
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
