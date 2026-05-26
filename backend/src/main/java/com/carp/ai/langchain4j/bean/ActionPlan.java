package com.carp.ai.langchain4j.bean;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class ActionPlan {
    @Description("Next interview stage. Must be one of ICE_BREAKING, BASIC_TECH, DEEP_DIVE, WRAP_UP.")
    private String nextStage;

    @Description("Concrete instruction for the interview agent about what to ask or how to follow up.")
    private String instruction;
}
