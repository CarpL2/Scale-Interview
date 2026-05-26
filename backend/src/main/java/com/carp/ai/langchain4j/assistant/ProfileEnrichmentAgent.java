package com.carp.ai.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel", 
        chatMemoryProvider = "chatMemoryProvider" 
)
public interface ProfileEnrichmentAgent {

    @SystemMessage({
            "你是一个专业的用户画像分析专家。",
            "你的任务是阅读用户与导师的这段【辅导问答记录】，从中精准提取出用户掌握得不扎实、存在疑惑或薄弱的【核心知识点】。",
            "例如：如果用户问了关于Redis持久化的问题，说明他对'Redis持久化机制'薄弱。如果问了JVM垃圾回收，就提炼出'JVM垃圾回收机制'。",
            "请直接返回一个字符串列表，每个字符串代表一个具体的薄弱知识点。",
            "如果聊天记录太短或完全没有发现明确的知识盲区，请返回空列表。切勿伪造弱点。"
    })
    @UserMessage("请分析我们的聊天记录，提炼出我的薄弱知识点列表。")
    List<String> extractWeaknesses(@MemoryId String memoryId);
}
