package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.InterviewAgentTrace;

import java.util.Optional;

public interface InterviewAgentTraceService extends IService<InterviewAgentTrace> {

    Optional<InterviewAgentTrace> getLatestTrace(Long userId, String sessionId);

    Optional<InterviewAgentTrace> getLatestCompletedTrace(Long userId, String sessionId);

    void updateLatestToolTrace(Long userId,
                               String sessionId,
                               String toolName,
                               String toolQuery,
                               boolean fallbackUsed,
                               String toolResultSummary);
}
