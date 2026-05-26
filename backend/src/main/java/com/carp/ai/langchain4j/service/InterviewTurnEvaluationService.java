package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.InterviewTurnEvaluation;

import java.util.List;

public interface InterviewTurnEvaluationService extends IService<InterviewTurnEvaluation> {
    List<InterviewTurnEvaluation> getEvaluationsBySessionId(Long userId, String sessionId);
}
