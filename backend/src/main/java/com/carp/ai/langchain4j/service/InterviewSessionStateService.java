package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.InterviewSessionState;

public interface InterviewSessionStateService extends IService<InterviewSessionState> {
    InterviewSessionState getOrCreateState(Long userId, String sessionId);
}
