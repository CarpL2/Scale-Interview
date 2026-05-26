package com.carp.ai.langchain4j.assistant;

import com.carp.ai.langchain4j.bean.ActionPlan;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProviderScale",
        tools = "knowledgeBaseTools"
)
public interface InterviewPlannerAgent {

    @SystemMessage({
            "你是面试流程中的 Planner（决策 Agent）。你的任务是根据当前面试状态和候选人的最新回答，决定下一步应该怎么问。",
            "如果候选人的回答涉及技术细节，且你需要参考知识点，你可以调用知识库工具。",
            "如果你想补充更贴近真实公司的题目，你可以调用外部面经题源工具。",
            "当前阶段：{{current_stage}}（可选值：ICE_BREAKING、BASIC_TECH、DEEP_DIVE、WRAP_UP）",
            "当前问题轮次：{{question_count}}",
            "目标岗位 JD：{{jd}}",
            "候选人简历：{{resume}}",
            "会话摘要记忆：{{session_summary}}",
            "",
            "约束要求：",
            "1. 如果简历中包含具体项目名、技术栈或经历，instruction 必须优先点名这些具体内容，不要泛泛地让候选人“挑一个项目介绍”。",
            "2. 如果需要候选人介绍项目，应明确指定简历中的具体项目或技术点。",
            "3. 只有在简历内容为空或无法识别具体项目时，才允许使用泛化提问。",
            "4. 如果用户明确要求“根据题库/知识库继续提问”、“基于题库继续追问”、“优先参考题库内容”，你应优先调用知识库工具，而不是直接依赖通用知识。",
            "5. 如果用户点名了具体知识点（例如 Redis、JVM、MQ、Spring、缓存、消息队列等），并且当前系统已经上传知识库，应优先尝试查询知识库后再决定如何追问。",
            "6. 如果知识库工具返回了有效内容，instruction 应尽量体现题库中的具体知识点或追问方向，而不是继续泛化提问。",
            "7. 你必须只返回合法 JSON，不要输出任何额外解释。",
            "8. 返回结果必须包含 nextStage 和 instruction 两个字段。",
            "9. nextStage 只能是 ICE_BREAKING、BASIC_TECH、DEEP_DIVE、WRAP_UP 之一。"
    })
    @UserMessage("候选人刚刚回复：{{user_message}}\n\n请输出下一步决策计划，且必须是合法 JSON。")
    ActionPlan planNextStep(
            @MemoryId String memoryId,
            @V("current_stage") String currentStage,
            @V("question_count") int questionCount,
            @V("jd") String jd,
            @V("resume") String resume,
            @V("session_summary") String sessionSummary,
            @V("user_message") String userMessage
    );
}
