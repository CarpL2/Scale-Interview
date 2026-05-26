package com.carp.ai.langchain4j.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeSearchResult {
    private boolean success;
    private boolean fallbackUsed;
    private String originalQuery;
    private String normalizedQuery;
    private List<String> queryCandidates = new ArrayList<>();
    private List<String> chunks = new ArrayList<>();
    private String errorMessage;
}
