package com.carp.ai.langchain4j.tools;

import com.carp.ai.langchain4j.bean.KnowledgeSearchResult;
import com.carp.ai.langchain4j.service.InterviewAgentTraceService;
import com.carp.ai.langchain4j.service.KnowledgeSearchService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KnowledgeBaseTools {

    private static final int KNOWLEDGE_SEARCH_TIMEOUT_SECONDS = 5;

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @Resource
    private InterviewAgentTraceService interviewAgentTraceService;

    @Tool("Search the interview knowledge base for standard answers, references, or facts needed to evaluate a candidate answer. Input should be a specific interview topic, for example: Redis persistence.")
    public String searchKnowledgeBase(@ToolMemoryId String memoryId, String queryStr) {
        try {
            System.out.println("TOOL CALLED searchKnowledgeBase | memoryId=" + memoryId + " | query=" + queryStr);
            Long userId = resolveUserId(memoryId);
            String sessionId = resolveSessionId(memoryId);
            KnowledgeSearchResult result = CompletableFuture
                    .supplyAsync(() -> knowledgeSearchService.search(userId, queryStr))
                    .get(KNOWLEDGE_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (result == null || !result.isSuccess()) {
                recordToolTrace(userId, sessionId, queryStr, true, "No relevant chunks returned.");
                return "Knowledge base did not return relevant content. Continue in generic interview mode and do not claim external evidence.";
            }

            recordToolTrace(
                    userId,
                    sessionId,
                    queryStr,
                    result.isFallbackUsed(),
                    result.getChunks().isEmpty() ? "No chunk summary." : result.getChunks().get(0)
            );
            String prefix = result.isFallbackUsed()
                    ? "[knowledge_search fallback_used=true]\n"
                    : "[knowledge_search fallback_used=false]\n";
            return prefix + String.join("\n---\n", result.getChunks());
        } catch (TimeoutException e) {
            return "Knowledge retrieval timed out. Continue in generic interview mode without interrupting the session.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Knowledge retrieval was interrupted. Continue in generic interview mode without interrupting the session.";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return "Knowledge base is unavailable: " + safeErrorMessage(cause)
                    + ". Continue in generic interview mode without interruption.";
        } catch (Exception e) {
            return "Knowledge base is unavailable: " + safeErrorMessage(e)
                    + ". Continue in generic interview mode without interruption.";
        }
    }

    @Tool("Call an external MCP interview-question service to fetch real company interview questions. Input must contain company and position.")
    public String fetchRealInterviewQuestionsFromMCP(String company, String position) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "mcp_client.py", company, position);
            pb.directory(new java.io.File("d:/project/AI-LangChain4j-backend/mcp-server"));
            Process process = pb.start();

            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "MCP interview-question service timed out. Please fall back to generic interview mode.";
            }

            String result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                return "MCP interview-question lookup failed. Fall back to generic interview mode. Error: " + error;
            }
            return result.trim();
        } catch (Exception e) {
            return "MCP service is unavailable. Fall back to generic interview mode. Cause: " + safeErrorMessage(e);
        }
    }

    private Long resolveUserId(String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            return 0L;
        }
        int separator = memoryId.indexOf('_');
        String userIdPart = separator >= 0 ? memoryId.substring(0, separator) : memoryId;
        try {
            return Long.parseLong(userIdPart);
        } catch (Exception e) {
            return 0L;
        }
    }

    private String resolveSessionId(String memoryId) {
        if (memoryId == null || memoryId.isBlank()) {
            return "default";
        }
        int separator = memoryId.indexOf('_');
        if (separator < 0 || separator + 1 >= memoryId.length()) {
            return "default";
        }
        String sessionId = memoryId.substring(separator + 1);
        if (sessionId.endsWith("_planner")) {
            sessionId = sessionId.substring(0, sessionId.length() - "_planner".length());
        }
        return sessionId;
    }

    private void recordToolTrace(Long userId,
                                 String sessionId,
                                 String toolQuery,
                                 boolean fallbackUsed,
                                 String resultSummary) {
        interviewAgentTraceService.updateLatestToolTrace(
                userId,
                sessionId,
                "searchKnowledgeBase",
                toolQuery,
                fallbackUsed,
                resultSummary
        );
    }

    private String safeErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
