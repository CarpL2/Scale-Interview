package com.carp.ai.langchain4j.service;

public interface InterviewEvaluationTask {

    void evaluateAnswerAsync(Long userId, String sessionId, String memoryId, String userMessage);
}
