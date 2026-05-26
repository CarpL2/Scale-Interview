package com.carp.ai.langchain4j.service.impl;

import com.carp.ai.langchain4j.assistant.AnswerEvaluationAgent;
import com.carp.ai.langchain4j.bean.TurnEvaluationResult;
import com.carp.ai.langchain4j.entity.InterviewAgentTrace;
import com.carp.ai.langchain4j.entity.InterviewSessionState;
import com.carp.ai.langchain4j.entity.InterviewTurnEvaluation;
import com.carp.ai.langchain4j.service.InterviewAgentTraceService;
import com.carp.ai.langchain4j.service.InterviewEvaluationTask;
import com.carp.ai.langchain4j.service.InterviewSessionStateService;
import com.carp.ai.langchain4j.service.InterviewTurnEvaluationService;
import com.carp.ai.langchain4j.service.ModelCallMetricService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InterviewEvaluationTaskImpl implements InterviewEvaluationTask {

    @Resource
    private AnswerEvaluationAgent answerEvaluationAgent;

    @Resource
    private InterviewSessionStateService sessionStateService;

    @Resource
    private InterviewTurnEvaluationService turnEvaluationService;

    @Resource
    private InterviewAgentTraceService agentTraceService;

    @Resource
    private ModelCallMetricService modelCallMetricService;

    @Async("interviewEvaluationExecutor")
    @Override
    public void evaluateAnswerAsync(Long userId, String sessionId, String memoryId, String userMessage) {
        long startTime = modelCallMetricService.start();
        try {
            if (userMessage == null || userMessage.length() < 5) {
                return;
            }

            String evalMemoryId = memoryId + "_turn_eval_" + System.nanoTime();
            TurnEvaluationResult result = answerEvaluationAgent.evaluateAnswer(evalMemoryId, userMessage);
            if (!isValidEvaluationResult(result)) {
                result = answerEvaluationAgent.evaluateAnswer(evalMemoryId + "_retry", userMessage);
                if (!isValidEvaluationResult(result)) {
                    modelCallMetricService.recordFailure(
                            userId,
                            sessionId,
                            "AnswerEvaluationAgent",
                            "qwenChatModel",
                            false,
                            startTime,
                            new IllegalStateException("Evaluation structured output validation failed twice")
                    );
                    return;
                }
            }
            modelCallMetricService.recordSuccess(userId, sessionId, "AnswerEvaluationAgent", "qwenChatModel", false, startTime);
            if (result == null) {
                return;
            }

            InterviewSessionState state = sessionStateService.getOrCreateState(userId, sessionId);
            InterviewAgentTrace latestTrace = agentTraceService.getLatestCompletedTrace(userId, sessionId)
                    .orElse(null);
            String latestQuestion = latestTrace == null
                    ? "见聊天历史"
                    : latestTrace.getAgentResponse();
            Integer turnIndex = latestTrace == null
                    ? state.getQuestionCount()
                    : latestTrace.getTurnIndex();

            if (turnIndex == null) {
                turnIndex = 0;
            }

            String question = latestQuestion == null || latestQuestion.isBlank()
                    ? "见聊天历史"
                    : latestQuestion;

            InterviewTurnEvaluation eval = new InterviewTurnEvaluation();
            eval.setUserId(userId);
            eval.setSessionId(sessionId);
            eval.setTurnIndex(turnIndex);
            eval.setQuestion(question);
            eval.setUserAnswer(userMessage);
            eval.setTechnicalScore(result.getTechnicalScore());
            eval.setCommunicationScore(result.getCommunicationScore());
            eval.setFeedback(result.getFeedback());
            eval.setExtractedWeaknesses(result.getExtractedWeaknesses());
            eval.setCreateTime(LocalDateTime.now());
            turnEvaluationService.save(eval);
        } catch (Exception e) {
            modelCallMetricService.recordFailure(userId, sessionId, "AnswerEvaluationAgent", "qwenChatModel", false, startTime, e);
            e.printStackTrace();
        }
    }

    private boolean isValidEvaluationResult(TurnEvaluationResult result) {
        if (result == null) {
            return false;
        }
        if (result.getTechnicalScore() == null || result.getCommunicationScore() == null) {
            return false;
        }
        if (result.getTechnicalScore() < 0 || result.getTechnicalScore() > 100) {
            return false;
        }
        if (result.getCommunicationScore() < 0 || result.getCommunicationScore() > 100) {
            return false;
        }
        return result.getFeedback() != null && !result.getFeedback().isBlank();
    }
}
