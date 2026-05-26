package com.carp.ai.langchain4j.assistant;

import com.carp.ai.langchain4j.bean.InterviewReport;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 面试评估 Agent，专门用于生成结构化的成绩单
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel", // 这里不用流式，因为我们要一次性返回完整的 JSON 对象
        chatMemoryProvider = "chatMemoryProvider" // 共享同一个记忆库，这样它就能看到刚才面试的所有对话！
)
public interface EvaluationAgent {

    @SystemMessage({
            "你是一个极其严格、资深的HR和技术总监。你的任务是根据刚才的面试记录、候选人的简历、岗位JD，以及【过程中系统自动打分的每一轮评估汇总】，生成最终的结构化面试评估报告。",
            "岗位要求：\n{{jd}}",
            "候选人简历：\n{{resume}}",
            "【过程评估汇总】：\n{{eval_summary}}",
            "【打分红线规则】：",
            "1. 请综合【过程评估汇总】中的得分情况，计算出一个合理的平均或加权分数作为最终技术得分和沟通得分。",
            "2. 总结出候选人的薄弱知识点，必须为每一个薄弱点提供详尽、准确的【正确参考解答】。",
            "请严格按照指定的结构返回合法的 JSON 格式报告。"
    })
    @UserMessage("面试已结束，请生成最终报告。")
    InterviewReport generateReport(
            @MemoryId String memoryId,
            @V("jd") String jd,
            @V("resume") String resume,
            @V("eval_summary") String evalSummary
    );
}
