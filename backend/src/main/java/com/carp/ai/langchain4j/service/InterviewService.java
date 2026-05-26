package com.carp.ai.langchain4j.service;

import com.carp.ai.langchain4j.bean.InterviewReport;
import com.carp.ai.langchain4j.bean.AgentDecisionSummary;
import reactor.core.publisher.Flux;

/**
 * 面试核心业务接口
 */
public interface InterviewService {
    /**
     * 与面试官对话
     * @param userId 用户ID（用于隔离画像和记忆）
     * @param userMessage 用户发送的消息
     * @return 面试官的回答
     */
    Flux<String> chat(Long userId, String sessionId, String userMessage);

    /**
     * 结束面试并生成评估报告
     * @param userId 用户ID
     * @return 结构化的评估报告
     */
    InterviewReport generateReport(Long userId, String sessionId);

    /**
     * 获取历史面试记录
     * @param userId 用户ID
     * @return 历史记录列表
     */
    java.util.List<com.carp.ai.langchain4j.entity.InterviewRecord> getHistory(Long userId);

    /**
     * 获取最近一轮 Agent 决策摘要，不返回用户输入和完整模型回复。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 最近一轮决策摘要
     */
    AgentDecisionSummary getLatestDecisionSummary(Long userId, String sessionId);
}
