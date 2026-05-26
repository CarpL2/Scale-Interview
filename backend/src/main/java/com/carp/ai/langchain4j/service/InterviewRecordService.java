package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.InterviewRecord;

public interface InterviewRecordService extends IService<InterviewRecord> {
    InterviewRecord getOrCreateBySessionId(Long userId, String sessionId);
}
