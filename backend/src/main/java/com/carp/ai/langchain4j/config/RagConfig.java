package com.carp.ai.langchain4j.config;

import com.carp.ai.langchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationScript;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Resource
    private MongoChatMemoryStore mongoChatMemoryStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Value("${app.rag.elasticsearch.url}")
    private String elasticsearchUrl;

    @Value("${app.rag.elasticsearch.index-name}")
    private String elasticsearchIndexName;

    @Value("${app.rag.retrieval.max-results:3}")
    private Integer maxResults;

    @Value("${app.rag.retrieval.min-score:0.6}")
    private Double minScore;

    @Bean
    public ChatMemoryProvider chatMemoryProviderScale() {
        return memoryId ->
                MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(mongoChatMemoryStore)
                        .build();
    }

    @Bean(destroyMethod = "close")
    RestClient elasticsearchRestClient() {
        return RestClient.builder(HttpHost.create(elasticsearchUrl)).build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStoreScale(RestClient elasticsearchRestClient) {
        return ElasticsearchEmbeddingStore.builder()
                .configuration(ElasticsearchConfigurationScript.builder().build())
                .restClient(elasticsearchRestClient)
                .indexName(elasticsearchIndexName)
                .build();
    }

    @Bean
    ContentRetriever contentRetrieverScale(EmbeddingStore<TextSegment> embeddingStoreScale) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStoreScale)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }
}
