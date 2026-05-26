package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.bean.InterviewReport;
import com.carp.ai.langchain4j.bean.AgentDecisionSummary;
import com.carp.ai.langchain4j.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "面试对话管理")
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    @Resource
    private InterviewService interviewService;

    @Operation(summary = "与王牌面试官对话")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestParam("userId") Long userId,
                             @RequestParam(value = "sessionId", required = false) String sessionId,
                             @RequestBody String message) {
        return interviewService.chat(userId, sessionId, message);
    }

    @Operation(summary = "结束面试并生成结构化评估报告")
    @PostMapping("/report")
    public InterviewReport generateReport(@RequestParam("userId") Long userId,
                                          @RequestParam(value = "sessionId", required = false) String sessionId) {
        return interviewService.generateReport(userId, sessionId);
    }

    @Operation(summary = "获取历史面试成绩单记录")
    @GetMapping("/history")
    public java.util.List<com.carp.ai.langchain4j.entity.InterviewRecord> getHistory(@RequestParam("userId") Long userId) {
        return interviewService.getHistory(userId);
    }

    @Operation(summary = "获取最近一轮 Agent 决策摘要")
    @GetMapping("/trace/latest")
    public AgentDecisionSummary getLatestDecisionSummary(@RequestParam("userId") Long userId,
                                                         @RequestParam(value = "sessionId", required = false) String sessionId) {
        return interviewService.getLatestDecisionSummary(userId, sessionId);
    }
}
