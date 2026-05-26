package com.carp.ai.langchain4j.service.impl;

import com.carp.ai.langchain4j.assistant.EvaluationAgent;
import com.carp.ai.langchain4j.assistant.InterviewAgent;
import com.carp.ai.langchain4j.assistant.InterviewPlannerAgent;
import com.carp.ai.langchain4j.bean.ActionPlan;
import com.carp.ai.langchain4j.bean.AgentDecisionSummary;
import com.carp.ai.langchain4j.bean.InterviewReport;
import com.carp.ai.langchain4j.entity.InterviewAgentTrace;
import com.carp.ai.langchain4j.entity.InterviewRecord;
import com.carp.ai.langchain4j.entity.InterviewSessionState;
import com.carp.ai.langchain4j.entity.InterviewTurnEvaluation;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import com.carp.ai.langchain4j.service.InterviewAgentTraceService;
import com.carp.ai.langchain4j.service.InterviewEvaluationTask;
import com.carp.ai.langchain4j.service.InterviewRecordService;
import com.carp.ai.langchain4j.service.InterviewService;
import com.carp.ai.langchain4j.service.InterviewSessionStateService;
import com.carp.ai.langchain4j.service.InterviewTurnEvaluationService;
import com.carp.ai.langchain4j.service.ModelCallMetricService;
import com.carp.ai.langchain4j.service.UserInterviewProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final Set<String> VALID_STAGES = Set.of(
            "ICE_BREAKING",
            "BASIC_TECH",
            "DEEP_DIVE",
            "WRAP_UP"
    );

    @Resource
    private InterviewAgent interviewAgent;

    @Resource
    private EvaluationAgent evaluationAgent;

    @Resource
    private InterviewPlannerAgent interviewPlannerAgent;

    @Resource
    private UserInterviewProfileService profileService;

    @Resource
    private InterviewRecordService interviewRecordService;

    @Resource
    private InterviewSessionStateService sessionStateService;

    @Resource
    private InterviewTurnEvaluationService turnEvaluationService;

    @Resource
    private InterviewAgentTraceService agentTraceService;

    @Resource
    private InterviewEvaluationTask interviewEvaluationTask;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ModelCallMetricService modelCallMetricService;

    @Override
    public Flux<String> chat(Long userId, String sessionId, String userMessage) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        String memoryId = userId + "_" + normalizedSessionId;
        String plannerMemoryId = memoryId + "_planner";

        interviewEvaluationTask.evaluateAnswerAsync(userId, normalizedSessionId, memoryId, userMessage);

        UserInterviewProfile profile = profileService.getByUserId(userId);
        InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, normalizedSessionId);

        String jd = textOrDefault(record != null ? record.getJdContent() : null, "暂无岗位要求");
        String resume = textOrDefault(record != null ? record.getResumeContent() : null, "暂无简历背景");
        String interviewStyle = textOrDefault(record != null ? record.getInterviewStyle() : null, "PROFESSIONAL");
        String username = textOrDefault(profile != null ? profile.getUserName() : null, "候选人");
        String weaknesses = textOrDefault(profile != null ? profile.getWeaknessProfile() : null, "暂无明显薄弱点");
        String stylePrompt = mapStyleToPrompt(interviewStyle);

        InterviewSessionState state = sessionStateService.getOrCreateState(userId, normalizedSessionId);
        int questionCountBeforePlan = state.getQuestionCount() == null ? 0 : state.getQuestionCount();
        String currentStage = normalizeStage(state.getCurrentStage(), questionCountBeforePlan);
        String sessionSummary = textOrDefault(state.getSessionSummary(), "No structured summary yet.");
        int plannedTurnIndex = questionCountBeforePlan + 1;

        ensurePendingTrace(userId, normalizedSessionId, plannedTurnIndex, currentStage, userMessage);

        long plannerStart = modelCallMetricService.start();
        PlannerResult plannerResult = safePlanNextStep(
                userId,
                normalizedSessionId,
                plannerStart,
                plannerMemoryId,
                currentStage,
                questionCountBeforePlan,
                jd,
                resume,
                sessionSummary,
                userMessage
        );
        ActionPlan actionPlan = plannerResult.actionPlan();

        state.setCurrentStage(actionPlan.getNextStage());
        state.setQuestionCount(questionCountBeforePlan + 1);
        state.setUpdateTime(LocalDateTime.now());
        sessionStateService.updateById(state);

        int turnIndex = state.getQuestionCount();
        String traceStage = state.getCurrentStage();
        String traceInstruction = actionPlan.getInstruction();
        String traceToolStatus = plannerResult.status();
        StringBuilder responseBuffer = new StringBuilder();

        long agentStart = modelCallMetricService.start();

        return interviewAgent.chat(memoryId, jd, resume, username, stylePrompt, weaknesses, traceInstruction, userMessage)
                .doOnNext(responseBuffer::append)
                .doOnError(error -> modelCallMetricService.recordFailure(
                        userId,
                        normalizedSessionId,
                        "InterviewAgent",
                        "qwenStreamingChatModel",
                        true,
                        agentStart,
                        error
                ))
                .doOnComplete(() -> saveAgentTrace(
                        userId,
                        normalizedSessionId,
                        turnIndex,
                        traceStage,
                        traceInstruction,
                        userMessage,
                        responseBuffer.toString(),
                        traceToolStatus
                ))
                .doOnComplete(() -> refreshSessionSummary(userId, normalizedSessionId))
                .doOnComplete(() -> modelCallMetricService.recordSuccess(
                        userId,
                        normalizedSessionId,
                        "InterviewAgent",
                        "qwenStreamingChatModel",
                        true,
                        agentStart
                ));
    }

    private PlannerResult safePlanNextStep(Long userId,
                                           String sessionId,
                                           long startTime,
                                           String memoryId,
                                           String currentStage,
                                           int questionCount,
                                           String jd,
                                           String resume,
                                           String sessionSummary,
                                           String userMessage) {
        try {
            ActionPlan parsed = interviewPlannerAgent.planNextStep(memoryId, currentStage, questionCount, jd, resume, sessionSummary, userMessage);
            if (!isValidPlannerResult(parsed)) {
                parsed = interviewPlannerAgent.planNextStep(memoryId, currentStage, questionCount, jd, resume, sessionSummary, userMessage);
                if (!isValidPlannerResult(parsed)) {
                    modelCallMetricService.recordFailure(
                            userId,
                            sessionId,
                            "InterviewPlannerAgent",
                            "qwenChatModel",
                            false,
                            startTime,
                            new IllegalStateException("Planner structured output validation failed twice")
                    );
                    return new PlannerResult(defaultActionPlan(currentStage, questionCount), "PLANNER_RETRY_VALIDATION_FALLBACK");
                }
            }
            modelCallMetricService.recordSuccess(userId, sessionId, "InterviewPlannerAgent", "qwenChatModel", false, startTime);
            return normalizePlannerResult(parsed, currentStage, questionCount, "PLANNER_OK");
        } catch (Exception e) {
            modelCallMetricService.recordFailure(userId, sessionId, "InterviewPlannerAgent", "qwenChatModel", false, startTime, e);
            return new PlannerResult(defaultActionPlan(currentStage, questionCount), "PLANNER_EXCEPTION_FALLBACK");
        }
    }

    private boolean isValidPlannerResult(ActionPlan plan) {
        if (plan == null) {
            return false;
        }
        if (plan.getNextStage() == null || plan.getNextStage().isBlank()) {
            return false;
        }
        return plan.getInstruction() != null && !plan.getInstruction().isBlank();
    }

    private void refreshSessionSummary(Long userId, String sessionId) {
        try {
            InterviewSessionState state = sessionStateService.getOrCreateState(userId, sessionId);
            List<InterviewTurnEvaluation> evaluations = turnEvaluationService.getEvaluationsBySessionId(userId, sessionId);
            InterviewAgentTrace latestTrace = agentTraceService.getLatestCompletedTrace(userId, sessionId).orElse(null);

            String stage = normalizeStage(state.getCurrentStage(), state.getQuestionCount() == null ? 0 : state.getQuestionCount());
            String discussedProjects = extractHighlights(latestTrace == null ? "" : latestTrace.getUserMessage());
            String discussedTech = extractHighlights(latestTrace == null ? "" : latestTrace.getAgentResponse());
            String weaknesses = mergeWeaknesses(evaluations);
            String pendingTopics = inferPendingTopics(stage, weaknesses);

            String summary = String.format(
                    "Current stage: %s%nDiscussed project/topic hints: %s%nRecent technical focus: %s%nObserved weaknesses: %s%nPending topics: %s",
                    stage,
                    emptyAsNone(discussedProjects),
                    emptyAsNone(discussedTech),
                    emptyAsNone(weaknesses),
                    pendingTopics
            );

            state.setSessionSummary(summary);
            state.setUpdateTime(LocalDateTime.now());
            sessionStateService.updateById(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractHighlights(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 120) {
            return cleaned.substring(0, 120) + "...";
        }
        return cleaned;
    }

    private String mergeWeaknesses(List<InterviewTurnEvaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return "";
        }
        List<String> weaknesses = new ArrayList<>();
        for (InterviewTurnEvaluation eval : evaluations) {
            if (eval.getExtractedWeaknesses() != null && !eval.getExtractedWeaknesses().isBlank()) {
                String value = eval.getExtractedWeaknesses().trim();
                if (!weaknesses.contains(value)) {
                    weaknesses.add(value);
                }
            }
        }
        return String.join("; ", weaknesses);
    }

    private String inferPendingTopics(String stage, String weaknesses) {
        if ("ICE_BREAKING".equals(stage)) {
            return "Project responsibilities, architecture choices, and one core project walkthrough.";
        }
        if ("BASIC_TECH".equals(stage)) {
            return "Project trade-offs, production issues, and one or two base technology follow-up questions.";
        }
        if ("DEEP_DIVE".equals(stage)) {
            return weaknesses == null || weaknesses.isBlank()
                    ? "JVM, JUC, Spring internals, middleware principles."
                    : "Revisit weak points and continue JVM/JUC/Spring/middleware depth checks.";
        }
        return "Wrap-up reflection, highlights, and final questions from candidate.";
    }

    private String emptyAsNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }

    private PlannerResult normalizePlannerResult(ActionPlan plan,
                                                 String currentStage,
                                                 int questionCount,
                                                 String initialStatus) {
        String status = initialStatus;
        if (plan == null) {
            return new PlannerResult(defaultActionPlan(currentStage, questionCount), "PLANNER_PARSE_FALLBACK");
        }

        String nextStage = plan.getNextStage();
        if (nextStage == null || !VALID_STAGES.contains(nextStage)) {
            plan.setNextStage(currentStage);
            status = "PLANNER_INVALID_STAGE_FALLBACK";
        }

        if (questionCount >= 12 && !"WRAP_UP".equals(plan.getNextStage())) {
            plan.setNextStage("WRAP_UP");
            plan.setInstruction(defaultInstruction("WRAP_UP", questionCount));
            status = "PLANNER_LATE_STAGE_WRAP_UP_FALLBACK";
        }

        if (questionCount < 3 && "WRAP_UP".equals(plan.getNextStage())) {
            plan.setNextStage(currentStage);
            plan.setInstruction("继续进行项目或技术追问，不要过早结束面试。");
            status = "PLANNER_EARLY_WRAP_UP_FALLBACK";
        }

        if (plan.getInstruction() == null || plan.getInstruction().isBlank()) {
            plan.setInstruction(defaultInstruction(plan.getNextStage(), questionCount));
            status = "PLANNER_EMPTY_INSTRUCTION_FALLBACK";
        }

        return new PlannerResult(plan, status);
    }

    private ActionPlan defaultActionPlan(String currentStage, int questionCount) {
        ActionPlan plan = new ActionPlan();
        String nextStage = defaultStage(questionCount, currentStage);
        plan.setNextStage(nextStage);
        plan.setInstruction(defaultInstruction(nextStage, questionCount));
        return plan;
    }

    private String defaultStage(int questionCount, String currentStage) {
        if (questionCount <= 1) {
            return "ICE_BREAKING";
        }
        if (questionCount <= 5) {
            return "BASIC_TECH";
        }
        if (questionCount <= 8) {
            return "DEEP_DIVE";
        }
        return "WRAP_UP";
    }

    private String defaultInstruction(String stage, int questionCount) {
        return switch (stage) {
            case "ICE_BREAKING" -> "进行开场确认，要求候选人先做个人情况和核心项目的简要介绍。";
            case "BASIC_TECH" -> "继续围绕候选人简历中的项目和技术点做延展追问，要求讲清职责、方案、取舍、风险和线上问题处理，不要过早切到泛化八股。";
            case "DEEP_DIVE" -> "进入基础技术核查阶段，请结合候选人简历中写到的技能点，主动追问 JVM、JUC、线程池、集合、Spring 或中间件原理，要求解释原理、使用场景和常见坑点。";
            case "WRAP_UP" -> "进入收尾阶段，请候选人补充项目亮点、个人反思和想问面试官的问题，然后准备生成评估报告。";
            default -> questionCount < 6
                    ? "继续围绕候选人的项目经历和简历技术点追问落地细节。"
                    : "开始补基础技术题，重点检查 JVM、JUC、集合、Spring 和中间件原理。";
        };
    }

    private String normalizeStage(String stage, int questionCount) {
        if (stage != null && VALID_STAGES.contains(stage)) {
            return stage;
        }
        return defaultStage(questionCount, "ICE_BREAKING");
    }

    private void saveAgentTrace(Long userId,
                                String sessionId,
                                Integer turnIndex,
                                String currentStage,
                                String plannerInstruction,
                                String userMessage,
                                String agentResponse,
                                String toolStatus) {
        try {
            InterviewAgentTrace trace = agentTraceService.lambdaQuery()
                    .eq(InterviewAgentTrace::getUserId, userId)
                    .eq(InterviewAgentTrace::getSessionId, sessionId)
                    .eq(InterviewAgentTrace::getTurnIndex, turnIndex)
                    .one();

            if (trace == null) {
                trace = new InterviewAgentTrace();
                trace.setUserId(userId);
                trace.setSessionId(sessionId);
                trace.setTurnIndex(turnIndex);
                trace.setCreateTime(LocalDateTime.now());
            }

            trace.setCurrentStage(currentStage);
            trace.setPlannerInstruction(plannerInstruction);
            trace.setUserMessage(userMessage);
            trace.setAgentResponse(agentResponse);
            trace.setToolStatus(toolStatus);

            if (trace.getId() == null) {
                agentTraceService.save(trace);
            } else {
                agentTraceService.updateById(trace);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ensurePendingTrace(Long userId,
                                    String sessionId,
                                    Integer turnIndex,
                                    String currentStage,
                                    String userMessage) {
        try {
            InterviewAgentTrace trace = agentTraceService.lambdaQuery()
                    .eq(InterviewAgentTrace::getUserId, userId)
                    .eq(InterviewAgentTrace::getSessionId, sessionId)
                    .eq(InterviewAgentTrace::getTurnIndex, turnIndex)
                    .one();
            if (trace != null) {
                return;
            }

            InterviewAgentTrace pendingTrace = new InterviewAgentTrace();
            pendingTrace.setUserId(userId);
            pendingTrace.setSessionId(sessionId);
            pendingTrace.setTurnIndex(turnIndex);
            pendingTrace.setCurrentStage(currentStage);
            pendingTrace.setUserMessage(userMessage);
            pendingTrace.setToolStatus("PLANNER_PENDING");
            pendingTrace.setCreateTime(LocalDateTime.now());
            agentTraceService.save(pendingTrace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String mapStyleToPrompt(String style) {
        if (style == null) {
            style = "PROFESSIONAL";
        }
        return switch (style.toUpperCase()) {
            case "STERN" -> "你的风格是【严格压力面】。像一位严谨的技术负责人一样追问答案中的漏洞和边界。";
            case "ENCOURAGING" -> "你的风格是【鼓励引导面】。先肯定，再引导候选人补充关键细节。";
            default -> "你的风格是【专业标准面】。保持客观、中立、专业，根据回答质量继续追问或反馈。";
        };
    }

    @Override
    public InterviewReport generateReport(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, normalizedSessionId);
        String memoryId = userId + "_" + normalizedSessionId;

        String jd = textOrDefault(record != null ? record.getJdContent() : null, "暂无岗位要求");
        String resume = textOrDefault(record != null ? record.getResumeContent() : null, "暂无简历背景");

        List<InterviewTurnEvaluation> evaluations = turnEvaluationService.getEvaluationsBySessionId(userId, normalizedSessionId);
        StringBuilder evalSummary = new StringBuilder();
        for (InterviewTurnEvaluation eval : evaluations) {
            evalSummary.append(String.format(
                    "第%d轮回答%n技术得分:%d, 沟通得分:%d%n问题:%s%n回答:%s%n反馈:%s%n薄弱点:%s%n---%n",
                    eval.getTurnIndex(),
                    eval.getTechnicalScore(),
                    eval.getCommunicationScore(),
                    eval.getQuestion(),
                    eval.getUserAnswer(),
                    eval.getFeedback(),
                    eval.getExtractedWeaknesses()
            ));
        }

        long startTime = modelCallMetricService.start();
        InterviewReport report;
        try {
            report = evaluationAgent.generateReport(memoryId, jd, resume, evalSummary.toString());
            modelCallMetricService.recordSuccess(userId, normalizedSessionId, "EvaluationAgent", "qwenChatModel", false, startTime);
        } catch (Exception e) {
            modelCallMetricService.recordFailure(userId, normalizedSessionId, "EvaluationAgent", "qwenChatModel", false, startTime, e);
            report = fallbackReport(evaluations);
        }

        try {
            String reportJson = objectMapper.writeValueAsString(report);
            if (record != null) {
                record.setEvaluationReport(reportJson);
                if (record.getCreateTime() == null) {
                    record.setCreateTime(LocalDateTime.now());
                }
                interviewRecordService.updateById(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return report;
    }

    private InterviewReport fallbackReport(List<InterviewTurnEvaluation> evaluations) {
        InterviewReport report = new InterviewReport();

        if (evaluations == null || evaluations.isEmpty()) {
            report.setTechnicalScore(0);
            report.setCommunicationScore(0);
            report.setOverallSummary("本次面试报告由兜底逻辑生成。当前缺少足够的有效问答数据，建议先完成至少 3 轮技术问答后再生成报告。");
            report.setSuggestions(List.of(
                    "先完成至少 3 轮完整技术问答，再重新生成报告。",
                    "优先围绕简历项目补充职责、方案取舍和线上问题处理过程。"
            ));
            report.setWeakKnowledgePoints(List.of());
            return report;
        }

        int technicalTotal = 0;
        int communicationTotal = 0;
        int technicalCount = 0;
        int communicationCount = 0;
        List<InterviewReport.KnowledgePoint> weakPoints = new ArrayList<>();

        for (InterviewTurnEvaluation eval : evaluations) {
            if (eval.getTechnicalScore() != null) {
                technicalTotal += eval.getTechnicalScore();
                technicalCount++;
            }
            if (eval.getCommunicationScore() != null) {
                communicationTotal += eval.getCommunicationScore();
                communicationCount++;
            }

            if (isWeakTurn(eval)) {
                InterviewReport.KnowledgePoint point = new InterviewReport.KnowledgePoint();
                point.setQuestion(textOrDefault(eval.getQuestion(), "见本轮面试问题"));
                point.setCorrectAnswer(buildFallbackCorrectAnswer(eval));
                weakPoints.add(point);
            }
        }

        int technicalScore = technicalCount == 0 ? 0 : Math.round((float) technicalTotal / technicalCount);
        int communicationScore = communicationCount == 0 ? 0 : Math.round((float) communicationTotal / communicationCount);

        report.setTechnicalScore(technicalScore);
        report.setCommunicationScore(communicationScore);
        report.setOverallSummary(String.format(
                "本次报告由规则兜底生成。系统基于 %d 轮过程评分汇总结果：技术平均分为 %d，沟通平均分为 %d。建议后续继续加强项目细节表达、基础原理解释和方案权衡能力。",
                evaluations.size(),
                technicalScore,
                communicationScore
        ));
        report.setSuggestions(buildFallbackSuggestions(technicalScore, communicationScore));
        report.setWeakKnowledgePoints(weakPoints);
        return report;
    }

    private boolean isWeakTurn(InterviewTurnEvaluation eval) {
        if (eval == null) {
            return false;
        }
        Integer technicalScore = eval.getTechnicalScore();
        String weaknesses = eval.getExtractedWeaknesses();
        return (technicalScore != null && technicalScore < 70)
                || (weaknesses != null && !weaknesses.isBlank());
    }

    private String buildFallbackCorrectAnswer(InterviewTurnEvaluation eval) {
        StringBuilder answer = new StringBuilder();
        String weaknesses = eval.getExtractedWeaknesses();
        if (weaknesses != null && !weaknesses.isBlank()) {
            answer.append("本轮暴露的薄弱点：").append(weaknesses).append("。");
        }
        String feedback = eval.getFeedback();
        if (feedback != null && !feedback.isBlank()) {
            answer.append("建议结合本轮反馈复盘：").append(feedback).append("。");
        }
        if (answer.isEmpty()) {
            answer.append("建议围绕问题补充核心原理、项目场景、方案取舍、边界情况和线上问题处理过程。");
        } else {
            answer.append("复盘时还应补充核心原理、项目落地细节和方案权衡。");
        }
        return answer.toString();
    }

    private List<String> buildFallbackSuggestions(int technicalScore, int communicationScore) {
        List<String> suggestions = new ArrayList<>();
        if (technicalScore < 70) {
            suggestions.add("优先补齐基础原理和关键技术细节，回答时避免只给结论。");
        } else {
            suggestions.add("保持技术表达准确性，并继续补充方案边界和性能权衡。");
        }

        if (communicationScore < 70) {
            suggestions.add("回答问题时使用背景、方案、结果、反思的结构，提升表达完整度。");
        } else {
            suggestions.add("继续保持清晰表达，并在项目复盘中补充更多量化结果。");
        }

        suggestions.add("围绕简历项目准备高频追问，例如架构设计、并发处理、故障恢复、监控告警和基础原理核查。");
        return suggestions;
    }

    @Override
    public List<InterviewRecord> getHistory(Long userId) {
        return interviewRecordService.lambdaQuery()
                .eq(InterviewRecord::getUserId, userId)
                .orderByDesc(InterviewRecord::getCreateTime)
                .list();
    }

    @Override
    public AgentDecisionSummary getLatestDecisionSummary(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        return agentTraceService.getLatestTrace(userId, normalizedSessionId)
                .map(this::toDecisionSummary)
                .orElseGet(AgentDecisionSummary::new);
    }

    private AgentDecisionSummary toDecisionSummary(InterviewAgentTrace trace) {
        AgentDecisionSummary summary = new AgentDecisionSummary();
        summary.setTurnIndex(trace.getTurnIndex());
        summary.setCurrentStage(trace.getCurrentStage());
        summary.setDecisionSummary(buildDecisionSummary(trace));
        summary.setToolStatus(trace.getToolStatus());
        summary.setCreateTime(trace.getCreateTime());
        return summary;
    }

    private String buildDecisionSummary(InterviewAgentTrace trace) {
        String stage = normalizeStage(trace.getCurrentStage(), trace.getTurnIndex() == null ? 0 : trace.getTurnIndex());
        String toolStatus = trace.getToolStatus();
        String fallbackPrefix = toolStatus != null && toolStatus.contains("FALLBACK")
                ? "Planner 已触发兜底策略，"
                : "Planner 已完成决策，";

        return switch (stage) {
            case "ICE_BREAKING" -> fallbackPrefix + "当前重点是开场确认和收集候选人背景。";
            case "BASIC_TECH" -> fallbackPrefix + "当前重点是围绕候选人项目经历继续追问职责边界、方案取舍和线上问题处理。";
            case "DEEP_DIVE" -> fallbackPrefix + "当前重点是补基础技术核查，围绕简历技能继续问 JVM、JUC、集合、Spring 和中间件原理。";
            case "WRAP_UP" -> fallbackPrefix + "当前重点是收尾总结，并准备生成结构化评估报告。";
            default -> fallbackPrefix + "当前重点是保持面试流程连续推进。";
        };
    }

    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "default" : sessionId;
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record PlannerResult(ActionPlan actionPlan, String status) {
    }
}
