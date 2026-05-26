package com.carp.ai.langchain4j.bean;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class TurnEvaluationResult {
    @Description("Technical score from 0 to 100.")
    private Integer technicalScore;

    @Description("Communication score from 0 to 100.")
    private Integer communicationScore;

    @Description("Short direct feedback about the current answer.")
    private String feedback;

    @Description("Extracted weakness points. Empty string if no obvious weakness is found.")
    private String extractedWeaknesses;
}
