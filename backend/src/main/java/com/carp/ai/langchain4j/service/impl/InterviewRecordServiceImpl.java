package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.InterviewRecord;
import com.carp.ai.langchain4j.mapper.InterviewRecordMapper;
import com.carp.ai.langchain4j.service.InterviewRecordService;
import org.springframework.stereotype.Service;

@Service
public class InterviewRecordServiceImpl extends ServiceImpl<InterviewRecordMapper, InterviewRecord> implements InterviewRecordService {

    @Override
    public InterviewRecord getOrCreateBySessionId(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return null;
        InterviewRecord record = this.lambdaQuery()
                .eq(InterviewRecord::getSessionId, sessionId)
                .one();
        if (record == null) {
            record = new InterviewRecord();
            record.setUserId(userId);
            record.setSessionId(sessionId);
            record.setInterviewStyle("PROFESSIONAL");
            // 注意：这里先不设置 createTime，等 report 生成时更新
            this.save(record);
        }
        return record;
    }
}
