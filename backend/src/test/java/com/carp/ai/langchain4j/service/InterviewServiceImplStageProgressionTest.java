package com.carp.ai.langchain4j.service;

import com.carp.ai.langchain4j.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterviewServiceImplStageProgressionTest {

    @Test
    void shouldProgressByQuestionCountWhenStateIsEmpty() throws Exception {
        InterviewServiceImpl service = new InterviewServiceImpl();
        Method method = InterviewServiceImpl.class.getDeclaredMethod("defaultStage", int.class, String.class);
        method.setAccessible(true);

        assertEquals("ICE_BREAKING", method.invoke(service, 0, "ICE_BREAKING"));
        assertEquals("ICE_BREAKING", method.invoke(service, 1, "ICE_BREAKING"));
        assertEquals("BASIC_TECH", method.invoke(service, 2, "ICE_BREAKING"));
        assertEquals("BASIC_TECH", method.invoke(service, 5, "ICE_BREAKING"));
        assertEquals("DEEP_DIVE", method.invoke(service, 6, "ICE_BREAKING"));
        assertEquals("DEEP_DIVE", method.invoke(service, 8, "ICE_BREAKING"));
        assertEquals("WRAP_UP", method.invoke(service, 9, "ICE_BREAKING"));
    }

    @Test
    void shouldMoveToWrapUpAfterDeepDiveWindow() throws Exception {
        InterviewServiceImpl service = new InterviewServiceImpl();
        Method method = InterviewServiceImpl.class.getDeclaredMethod("defaultStage", int.class, String.class);
        method.setAccessible(true);

        assertEquals("WRAP_UP", method.invoke(service, 9, "DEEP_DIVE"));
        assertEquals("WRAP_UP", method.invoke(service, 9, "BASIC_TECH"));
    }
}
