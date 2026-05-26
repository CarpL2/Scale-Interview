package com.carp.ai.langchain4j.service.impl;

import com.carp.ai.langchain4j.service.KnowledgeIndexService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationScript;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeIndexServiceImpl implements KnowledgeIndexService {

    private final Map<Long, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    @Resource
    private RestClient elasticsearchRestClient;

    @Resource
    private EmbeddingModel embeddingModel;

    @Value("${app.rag.elasticsearch.index-name}")
    private String baseIndexName;

    @Override
    public String resolveIndexName(Long userId) {
        Long safeUserId = userId == null || userId <= 0 ? 0L : userId;
        return baseIndexName + "-user-" + safeUserId;
    }

    @Override
    public EmbeddingStore<TextSegment> embeddingStore(Long userId) {
        Long safeUserId = userId == null || userId <= 0 ? 0L : userId;
        return storeCache.computeIfAbsent(safeUserId, this::createStore);
    }

    private EmbeddingStore<TextSegment> createStore(Long userId) {
        return ElasticsearchEmbeddingStore.builder()
                .configuration(ElasticsearchConfigurationScript.builder().build())
                .restClient(elasticsearchRestClient)
                .indexName(resolveIndexName(userId))
                .build();
    }
}
