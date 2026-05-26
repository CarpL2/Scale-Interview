package com.carp.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetrieverScale"
)
public interface TutorAgent {

    @SystemMessage({
            "你是一个极其耐心、专业的 AI 知识导师（Tutor）。",
            "你的任务是帮助用户解答他们在技术、面试中遇到的疑惑。你应该尽量使用清晰易懂的语言，辅以简单的例子进行说明。",
            "你可以查阅提供的知识库内容来确保你的回答准确。如果知识库中没有，你可以根据你的基础知识进行解答。",
            "请使用 Markdown 格式返回。请一步步引导用户，多用鼓励的话语，绝对不能像严厉的面试官那样给用户施加压力或打分。"
    })
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
