package com.carp.ai.langchain4j.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeBaseStatus {
    private String indexName;
    private Long documentCount;
    private List<String> samples = new ArrayList<>();
}
