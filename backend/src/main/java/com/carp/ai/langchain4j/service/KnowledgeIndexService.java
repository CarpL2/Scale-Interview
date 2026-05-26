package com.carp.ai.langchain4j.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface KnowledgeIndexService {

    String resolveIndexName(Long userId);

    EmbeddingStore<TextSegment> embeddingStore(Long userId);
}
