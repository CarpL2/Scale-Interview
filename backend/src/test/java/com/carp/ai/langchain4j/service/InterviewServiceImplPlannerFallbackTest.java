package com.carp.ai.langchain4j.service;

import com.carp.ai.langchain4j.bean.ActionPlan;
import com.carp.ai.langchain4j.service.impl.InterviewServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InterviewServiceImplPlannerFallbackTest {

    @Test
    void shouldFallbackToDefaultInstructionWhenPlannerInstructionIsBlank() throws Exception {
        InterviewServiceImpl service = new InterviewServiceImpl();
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        ActionPlan plan = new ActionPlan();
        plan.setNextStage("BASIC_TECH");
        plan.setInstruction("   ");

        Method method = InterviewServiceImpl.class.getDeclaredMethod(
                "normalizePlannerResult",
                ActionPlan.class,
                String.class,
                int.class,
                String.class
        );
        method.setAccessible(true);

        Object result = method.invoke(service, plan, "BASIC_TECH", 2, "PLANNER_OK");
        Method actionPlanMethod = result.getClass().getDeclaredMethod("actionPlan");
        Method statusMethod = result.getClass().getDeclaredMethod("status");
        actionPlanMethod.setAccessible(true);
        statusMethod.setAccessible(true);

        ActionPlan normalized = (ActionPlan) actionPlanMethod.invoke(result);
        String status = (String) statusMethod.invoke(result);

        assertNotNull(normalized);
        assertEquals("PLANNER_EMPTY_INSTRUCTION_FALLBACK", status);
        assertEquals("BASIC_TECH", normalized.getNextStage());
        assertEquals("继续围绕候选人简历中的项目和技术点做延展追问，要求讲清职责、方案、取舍、风险和线上问题处理，不要过早切到泛化八股。", normalized.getInstruction());
    }

    @Test
    void shouldBlockEarlyWrapUpInFirstRounds() throws Exception {
        InterviewServiceImpl service = new InterviewServiceImpl();
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        ActionPlan plan = new ActionPlan();
        plan.setNextStage("WRAP_UP");
        plan.setInstruction("结束面试");

        Method method = InterviewServiceImpl.class.getDeclaredMethod(
                "normalizePlannerResult",
                ActionPlan.class,
                String.class,
                int.class,
                String.class
        );
        method.setAccessible(true);

        Object result = method.invoke(service, plan, "BASIC_TECH", 1, "PLANNER_OK");
        Method actionPlanMethod = result.getClass().getDeclaredMethod("actionPlan");
        Method statusMethod = result.getClass().getDeclaredMethod("status");
        actionPlanMethod.setAccessible(true);
        statusMethod.setAccessible(true);

        ActionPlan normalized = (ActionPlan) actionPlanMethod.invoke(result);
        String status = (String) statusMethod.invoke(result);

        assertEquals("PLANNER_EARLY_WRAP_UP_FALLBACK", status);
        assertEquals("BASIC_TECH", normalized.getNextStage());
    }
}
