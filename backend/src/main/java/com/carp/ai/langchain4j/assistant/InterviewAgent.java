package com.carp.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * AceInterviewer 核心 Agent 接口
 */
@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel", // 改用流式模型
        chatMemoryProvider = "chatMemoryProviderScale"
)
public interface InterviewAgent {

    /**
     * 开始面试（注入背景信息）
     */
    @SystemMessage(fromResource = "interview-prompt-template.txt")
    Flux<String> startInterview(
            @MemoryId String memoryId,
            @V("jd") String jd,
            @V("resume") String resume,
            @V("username") String username,
            @V("style") String style,
            @V("weaknesses") String weaknesses,
            @V("planner_instruction") String plannerInstruction,
            @UserMessage String message
    );

    /**
     * 继续面试
     */
    @SystemMessage(fromResource = "interview-prompt-template.txt")
    Flux<String> chat(
            @MemoryId String memoryId,
            @V("jd") String jd,
            @V("resume") String resume,
            @V("username") String username,
            @V("style") String style,
            @V("weaknesses") String weaknesses,
            @V("planner_instruction") String plannerInstruction,
            @UserMessage String message
    );
}
