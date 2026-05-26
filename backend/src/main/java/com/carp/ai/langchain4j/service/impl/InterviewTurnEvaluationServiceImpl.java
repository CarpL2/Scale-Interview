package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.InterviewTurnEvaluation;
import com.carp.ai.langchain4j.mapper.InterviewTurnEvaluationMapper;
import com.carp.ai.langchain4j.service.InterviewTurnEvaluationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewTurnEvaluationServiceImpl extends ServiceImpl<InterviewTurnEvaluationMapper, InterviewTurnEvaluation> implements InterviewTurnEvaluationService {

    @Override
    public List<InterviewTurnEvaluation> getEvaluationsBySessionId(Long userId, String sessionId) {
        return this.lambdaQuery()
                .eq(InterviewTurnEvaluation::getUserId, userId)
                .eq(InterviewTurnEvaluation::getSessionId, sessionId)
                .orderByAsc(InterviewTurnEvaluation::getTurnIndex)
                .list();
    }
}
