package com.carp.ai.langchain4j.service;

import reactor.core.publisher.Flux;

public interface TutorService {
    Flux<String> chat(Long userId, String sessionId, String userMessage);
    String enrichProfile(Long userId, String sessionId);
    String clearWeaknessProfile(Long userId);
}
