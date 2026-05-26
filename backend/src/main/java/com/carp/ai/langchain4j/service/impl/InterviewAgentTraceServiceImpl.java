package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.InterviewAgentTrace;
import com.carp.ai.langchain4j.mapper.InterviewAgentTraceMapper;
import com.carp.ai.langchain4j.service.InterviewAgentTraceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InterviewAgentTraceServiceImpl
        extends ServiceImpl<InterviewAgentTraceMapper, InterviewAgentTrace>
        implements InterviewAgentTraceService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureToolTraceColumns() {
        addColumnIfMissing("tool_name", "VARCHAR(64) NULL COMMENT 'Latest tool name'");
        addColumnIfMissing("tool_query", "VARCHAR(500) NULL COMMENT 'Latest tool query'");
        addColumnIfMissing("tool_fallback_used", "TINYINT(1) NULL COMMENT 'Whether fallback retrieval was used'");
        addColumnIfMissing("tool_result_summary", "VARCHAR(500) NULL COMMENT 'Latest tool result summary'");
    }

    @Override
    public Optional<InterviewAgentTrace> getLatestTrace(Long userId, String sessionId) {
        return Optional.ofNullable(this.lambdaQuery()
                .eq(InterviewAgentTrace::getUserId, userId)
                .eq(InterviewAgentTrace::getSessionId, sessionId)
                .orderByDesc(InterviewAgentTrace::getTurnIndex)
                .last("LIMIT 1")
                .one());
    }

    @Override
    public Optional<InterviewAgentTrace> getLatestCompletedTrace(Long userId, String sessionId) {
        return Optional.ofNullable(this.lambdaQuery()
                .eq(InterviewAgentTrace::getUserId, userId)
                .eq(InterviewAgentTrace::getSessionId, sessionId)
                .isNotNull(InterviewAgentTrace::getAgentResponse)
                .ne(InterviewAgentTrace::getAgentResponse, "")
                .orderByDesc(InterviewAgentTrace::getTurnIndex)
                .last("LIMIT 1")
                .one());
    }

    @Override
    public void updateLatestToolTrace(Long userId,
                                      String sessionId,
                                      String toolName,
                                      String toolQuery,
                                      boolean fallbackUsed,
                                      String toolResultSummary) {
        try {
            InterviewAgentTrace trace = getLatestTrace(userId, sessionId).orElse(null);
            if (trace == null) {
                return;
            }
            trace.setToolName(toolName);
            trace.setToolQuery(truncate(toolQuery, 500));
            trace.setToolFallbackUsed(fallbackUsed);
            trace.setToolResultSummary(truncate(toolResultSummary, 500));
            updateById(trace);
        } catch (Exception ignored) {
            // Tool trace should not break the main business flow.
        }
    }

    private void addColumnIfMissing(String columnName, String columnDefinition) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE interview_agent_trace ADD COLUMN " + columnName + " " + columnDefinition
            );
        } catch (Exception ignored) {
            // Ignore duplicate-column and similar initialization exceptions.
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
