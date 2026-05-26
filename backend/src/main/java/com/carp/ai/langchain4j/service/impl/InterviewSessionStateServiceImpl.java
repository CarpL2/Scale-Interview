package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.InterviewSessionState;
import com.carp.ai.langchain4j.mapper.InterviewSessionStateMapper;
import com.carp.ai.langchain4j.service.InterviewSessionStateService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InterviewSessionStateServiceImpl
        extends ServiceImpl<InterviewSessionStateMapper, InterviewSessionState>
        implements InterviewSessionStateService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSessionSummaryColumn() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE interview_session_state ADD COLUMN session_summary TEXT NULL COMMENT 'Structured session summary memory'"
            );
        } catch (Exception ignored) {
            // Ignore duplicate-column and similar initialization exceptions.
        }
    }

    @Override
    public InterviewSessionState getOrCreateState(Long userId, String sessionId) {
        InterviewSessionState state = this.lambdaQuery()
                .eq(InterviewSessionState::getUserId, userId)
                .eq(InterviewSessionState::getSessionId, sessionId)
                .one();

        if (state == null) {
            state = new InterviewSessionState();
            state.setUserId(userId);
            state.setSessionId(sessionId);
            state.setCurrentStage("ICE_BREAKING");
            state.setQuestionCount(0);
            state.setSessionSummary("No structured summary yet.");
            state.setCreateTime(LocalDateTime.now());
            state.setUpdateTime(LocalDateTime.now());
            this.save(state);
        }
        return state;
    }
}
