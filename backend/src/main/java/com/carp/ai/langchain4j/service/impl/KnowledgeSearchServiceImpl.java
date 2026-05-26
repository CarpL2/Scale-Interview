package com.carp.ai.langchain4j.service.impl;

import com.carp.ai.langchain4j.bean.KnowledgeSearchDebugView;
import com.carp.ai.langchain4j.bean.KnowledgeSearchResult;
import com.carp.ai.langchain4j.service.KnowledgeIndexService;
import com.carp.ai.langchain4j.service.KnowledgeSearchService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private static final int FALLBACK_MAX_RESULTS = 5;
    private static final double FALLBACK_MIN_SCORE = 0.45D;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private KnowledgeIndexService knowledgeIndexService;

    @Value("${app.rag.retrieval.max-results:3}")
    private Integer maxResults;

    @Value("${app.rag.retrieval.min-score:0.6}")
    private Double minScore;

    @Override
    public KnowledgeSearchResult search(Long userId, String query) {
        return retrieveInternal(userId, query);
    }

    @Override
    public KnowledgeSearchDebugView debugSearch(Long userId, String query) {
        KnowledgeSearchResult result = retrieveInternal(userId, query);
        KnowledgeSearchDebugView view = new KnowledgeSearchDebugView();
        view.setOriginalQuery(result.getOriginalQuery());
        view.setNormalizedQuery(result.getNormalizedQuery());
        view.setQueryCandidates(result.getQueryCandidates());
        view.setFallbackUsed(result.isFallbackUsed());
        view.setHitCount(result.getChunks().size());
        view.setChunks(result.getChunks());
        return view;
    }

    private KnowledgeSearchResult retrieveInternal(Long userId, String query) {
        String normalizedQuery = normalizeQuery(query);
        List<String> candidates = buildQueryCandidates(normalizedQuery);

        ContentRetriever primaryRetriever = buildRetriever(userId, maxResults, minScore);
        for (String candidate : candidates) {
            List<Content> primary = primaryRetriever.retrieve(Query.from(candidate));
            if (primary != null && !primary.isEmpty()) {
                return toResult(query, normalizedQuery, candidates, false, primary, null);
            }
        }

        ContentRetriever relaxedRetriever = buildRetriever(
                userId,
                Math.max(maxResults, FALLBACK_MAX_RESULTS),
                FALLBACK_MIN_SCORE
        );

        for (String candidate : candidates) {
            List<Content> fallback = relaxedRetriever.retrieve(Query.from(candidate));
            if (fallback != null && !fallback.isEmpty()) {
                return toResult(query, normalizedQuery, candidates, true, fallback, null);
            }
        }

        return toResult(
                query,
                normalizedQuery,
                candidates,
                true,
                List.of(),
                "No relevant chunks were recalled from the user knowledge base."
        );
    }

    private ContentRetriever buildRetriever(Long userId, int localMaxResults, double localMinScore) {
        EmbeddingStore<TextSegment> store = knowledgeIndexService.embeddingStore(userId);
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(localMaxResults)
                .minScore(localMinScore)
                .build();
    }

    private KnowledgeSearchResult toResult(String originalQuery,
                                           String normalizedQuery,
                                           List<String> candidates,
                                           boolean fallbackUsed,
                                           List<Content> contents,
                                           String errorMessage) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setOriginalQuery(originalQuery);
        result.setNormalizedQuery(normalizedQuery);
        result.setQueryCandidates(new ArrayList<>(candidates));
        result.setFallbackUsed(fallbackUsed);
        result.setSuccess(contents != null && !contents.isEmpty());
        result.setErrorMessage(errorMessage);

        if (contents != null) {
            for (Content content : contents) {
                String text = content.textSegment().text();
                text = text == null ? "" : text.replaceAll("\\s+", " ").trim();
                if (text.length() > 300) {
                    text = text.substring(0, 300) + "...";
                }
                if (!text.isBlank()) {
                    result.getChunks().add(text);
                }
            }
        }
        return result;
    }

    private List<String> buildQueryCandidates(String normalizedQuery) {
        List<String> candidates = new ArrayList<>();
        if (!normalizedQuery.isBlank()) {
            candidates.add(normalizedQuery);
        }

        String keywordOnly = stripBoilerplate(normalizedQuery);
        if (!keywordOnly.isBlank() && !candidates.contains(keywordOnly)) {
            candidates.add(keywordOnly);
        }

        String compact = keywordOnly
                .replace("面试题", "")
                .replace("八股", "")
                .replace("knowledge", "")
                .replace("interview", "")
                .trim();
        if (!compact.isBlank() && !candidates.contains(compact)) {
            candidates.add(compact);
        }
        return candidates;
    }

    private String normalizeQuery(String queryStr) {
        if (queryStr == null) {
            return "";
        }
        return queryStr
                .replace('\u3000', ' ')
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String stripBoilerplate(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        String stripped = lower
                .replace("please", "")
                .replace("search", "")
                .replace("knowledge base", "")
                .replace("interview", "")
                .replace("question", "")
                .replace("questions", "")
                .replace("topic", "")
                .replace("about", "")
                .replace("for", "");

        stripped = stripped
                .replace("请", "")
                .replace("帮我", "")
                .replace("查询", "")
                .replace("检索", "")
                .replace("知识库", "")
                .replace("面试", "")
                .replace("题目", "")
                .replace("问题", "")
                .replace("关于", "");

        return stripped.replaceAll("\\s{2,}", " ").trim();
    }
}
