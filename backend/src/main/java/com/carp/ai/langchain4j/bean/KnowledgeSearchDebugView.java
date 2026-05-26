package com.carp.ai.langchain4j.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeSearchDebugView {
    private String originalQuery;
    private String normalizedQuery;
    private List<String> queryCandidates = new ArrayList<>();
    private boolean fallbackUsed;
    private int hitCount;
    private List<String> chunks = new ArrayList<>();
}
