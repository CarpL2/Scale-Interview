package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "AI 知识导师")
@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    @Resource
    private TutorService tutorService;

    @Operation(summary = "与AI导师聊天（流式）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("userId") Long userId,
                             @RequestParam(value = "sessionId", required = false) String sessionId,
                             @RequestBody String userMessage) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }
        return tutorService.chat(userId, sessionId, userMessage);
    }

    @Operation(summary = "提取薄弱点并丰富画像")
    @PostMapping("/enrich-profile")
    public String enrichProfile(@RequestParam("userId") Long userId,
                                @RequestParam("sessionId") String sessionId) {
        return tutorService.enrichProfile(userId, sessionId);
    }

    @Operation(summary = "清空用户薄弱画像")
    @PostMapping("/clear-profile")
    public String clearProfile(@RequestParam("userId") Long userId) {
        return tutorService.clearWeaknessProfile(userId);
    }
}
