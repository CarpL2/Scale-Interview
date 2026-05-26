package com.carp.ai.langchain4j.service;

import com.carp.ai.langchain4j.bean.KnowledgeSearchResult;
import com.carp.ai.langchain4j.bean.KnowledgeSearchDebugView;

public interface KnowledgeSearchService {

    KnowledgeSearchResult search(Long userId, String query);

    KnowledgeSearchDebugView debugSearch(Long userId, String query);
}
