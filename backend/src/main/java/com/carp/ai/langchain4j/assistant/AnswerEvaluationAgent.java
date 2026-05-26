package com.carp.ai.langchain4j.assistant;

import com.carp.ai.langchain4j.bean.TurnEvaluationResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel", 
        chatMemoryProvider = "chatMemoryProviderScale" 
)
public interface AnswerEvaluationAgent {

    @SystemMessage({
            "你是一个严格的面试考官。你的任务是针对刚才候选人的一轮回答进行快速打分和评价。",
            "你只能评估给定的【问题】和【候选人回答】。不需要关心之前的历史，除非回答中提及。",
            "请严格按照结构返回合法的 JSON 格式。如果回答极度敷衍或不知道，必须给出0-40的低分。",
            "JSON字段要求：",
            "- technicalScore (整数0-100)：技术得分",
            "- communicationScore (整数0-100)：沟通得分",
            "- feedback (字符串)：对本次回答的直接评价，指出哪里对哪里错，不超过50字",
            "- extractedWeaknesses (字符串)：如果发现明显的知识盲区，简短列出(例如'Redis持久化')；如果没有，返回空字符串。"
    })
    @UserMessage("候选人刚刚回答: {{user_answer}}\n\n请结合上下文评估候选人的这个回答。")
    TurnEvaluationResult evaluateAnswer(
            @MemoryId String memoryId,
            @V("user_answer") String userAnswer
    );
}
