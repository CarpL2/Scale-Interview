package com.carp.ai.langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewPlannerPromptContractTest {

    @Test
    void plannerPromptShouldRequireResumeSpecificQuestions() throws Exception {
        Method method = InterviewPlannerAgent.class.getDeclaredMethod(
                "planNextStep",
                String.class,
                String.class,
                int.class,
                String.class,
                String.class,
                String.class
        );

        SystemMessage systemMessage = method.getAnnotation(SystemMessage.class);
        String prompt = String.join("\n", Arrays.asList(systemMessage.value()));

        assertTrue(prompt.contains("候选人简历：{{resume}}"));
        assertTrue(prompt.contains("instruction 必须优先点名这些具体内容"));
        assertTrue(prompt.contains("才允许使用泛化提问"));
    }
}
