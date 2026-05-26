package com.carp.ai.langchain4j.bean;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

/**
 * 面试评估报告
 */
@Data
public class InterviewReport {
    
    @Description("候选人的技术和专业能力得分，满分100分")
    private Integer technicalScore;

    @Description("候选人的沟通表达能力得分，满分100分")
    private Integer communicationScore;

    @Description("面试整体评价，字数在50到200字之间")
    private String overallSummary;

    @Description("给候选人的后续改进建议，请列出2到4点建议")
    private List<String> suggestions;

    @Description("面试中候选人答错或答得不好的知识点总结，以及正确的参考解答")
    private List<KnowledgePoint> weakKnowledgePoints;

    @Data
    public static class KnowledgePoint {
        @Description("候选人答错或答得不完整的具体面试问题")
        private String question;

        @Description("详细的正确解答和原理说明，用于帮助候选人复盘学习")
        private String correctAnswer;
    }
}
