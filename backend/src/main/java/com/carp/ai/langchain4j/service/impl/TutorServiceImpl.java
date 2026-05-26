package com.carp.ai.langchain4j.service.impl;

import com.carp.ai.langchain4j.assistant.ProfileEnrichmentAgent;
import com.carp.ai.langchain4j.assistant.TutorAgent;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import com.carp.ai.langchain4j.service.ModelCallMetricService;
import com.carp.ai.langchain4j.service.TutorService;
import com.carp.ai.langchain4j.service.UserInterviewProfileService;
import com.carp.ai.langchain4j.util.WeaknessProfileSanitizer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class TutorServiceImpl implements TutorService {

    @Resource
    private TutorAgent tutorAgent;

    @Resource
    private ProfileEnrichmentAgent profileEnrichmentAgent;

    @Resource
    private UserInterviewProfileService profileService;

    @Resource
    private ModelCallMetricService modelCallMetricService;

    @Override
    public Flux<String> chat(Long userId, String sessionId, String userMessage) {
        String memoryId = userId + "_tutor_" + sessionId;
        long startTime = modelCallMetricService.start();
        return tutorAgent.chat(memoryId, userMessage)
                .doOnError(error -> modelCallMetricService.recordFailure(
                        userId,
                        sessionId,
                        "TutorAgent",
                        "qwenStreamingChatModel",
                        true,
                        startTime,
                        error
                ))
                .doOnComplete(() -> modelCallMetricService.recordSuccess(
                        userId,
                        sessionId,
                        "TutorAgent",
                        "qwenStreamingChatModel",
                        true,
                        startTime
                ));
    }

    @Override
    public String enrichProfile(Long userId, String sessionId) {
        String memoryId = userId + "_tutor_" + sessionId;
        long startTime = modelCallMetricService.start();
        List<String> weaknesses;
        try {
            weaknesses = WeaknessProfileSanitizer.sanitize(
                    profileEnrichmentAgent.extractWeaknesses(memoryId)
            );
            modelCallMetricService.recordSuccess(userId, sessionId, "ProfileEnrichmentAgent", "qwenChatModel", false, startTime);
        } catch (Exception e) {
            modelCallMetricService.recordFailure(userId, sessionId, "ProfileEnrichmentAgent", "qwenChatModel", false, startTime, e);
            throw e;
        }

        if (!weaknesses.isEmpty()) {
            UserInterviewProfile profile = profileService.getByUserId(userId);
            String mergedProfile = WeaknessProfileSanitizer.mergeToProfileText(
                    profile.getWeaknessProfile(),
                    weaknesses
            );
            profile.setWeaknessProfile(mergedProfile);
            profileService.updateById(profile);
            return "成功提取 " + weaknesses.size() + " 个薄弱点并更新至画像。";
        }
        return "未发现明显的知识薄弱点，画像无需更新。";
    }

    @Override
    public String clearWeaknessProfile(Long userId) {
        UserInterviewProfile profile = profileService.getByUserId(userId);
        profile.setWeaknessProfile("");
        profileService.updateById(profile);
        return "薄弱画像已清空。";
    }
}
