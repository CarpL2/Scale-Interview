package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.bean.KnowledgeBaseStatus;
import com.carp.ai.langchain4j.bean.KnowledgeSearchDebugView;
import com.carp.ai.langchain4j.service.KnowledgeIndexService;
import com.carp.ai.langchain4j.service.KnowledgeSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Tag(name = "Knowledge RAG Management")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final int MAX_CHUNK_LENGTH = 800;
    private static final int CHUNK_OVERLAP = 120;
    private static final int MIN_PREFERRED_SPLIT_DISTANCE = 200;
    private static final Pattern MULTI_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern MULTI_SPACES = Pattern.compile("[\\t\\x0B\\f\\r ]+");

    @Resource
    private RestClient elasticsearchRestClient;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @Resource
    private KnowledgeIndexService knowledgeIndexService;

    @Operation(summary = "Clear user knowledge base")
    @PostMapping("/clear")
    public String clearKnowledgeBase(@RequestParam("userId") Long userId) {
        String indexName = knowledgeIndexService.resolveIndexName(userId);
        try {
            Request deleteRequest = new Request(HttpDelete.METHOD_NAME, "/" + indexName);
            elasticsearchRestClient.performRequest(deleteRequest);
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("index_not_found_exception")) {
                return "Failed to clear knowledge base: " + e.getMessage();
            }
        }

        try {
            ensureKnowledgeIndex(indexName);
            return "Knowledge base " + indexName + " was cleared and recreated successfully.";
        } catch (Exception e) {
            return "Knowledge base was deleted, but index recreation failed: " + e.getMessage();
        }
    }

    @Operation(summary = "Upload user knowledge file and ingest into Elasticsearch")
    @PostMapping("/upload")
    public String uploadKnowledge(@RequestParam("userId") Long userId,
                                  @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Uploaded file is empty.";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return "Uploaded file name is empty.";
        }

        String indexName = knowledgeIndexService.resolveIndexName(userId);

        try {
            ensureKnowledgeIndex(indexName);

            DocumentParser parser = createParser(fileName);
            Document document = parser.parse(file.getInputStream());
            List<TextSegment> segments = buildSegments(fileName, document, userId);
            if (segments.isEmpty()) {
                return "Parsed document is empty, nothing was written into the knowledge base.";
            }

            EmbeddingStore<TextSegment> store = knowledgeIndexService.embeddingStore(userId);
            store.addAll(
                    embeddingModel.embedAll(segments).content(),
                    segments
            );

            return "Knowledge file " + fileName + " uploaded successfully with " + segments.size() + " chunks.";
        } catch (Exception e) {
            return "Vectorization failed: " + safeMessage(e);
        }
    }

    @Operation(summary = "View user knowledge base status and sample chunks")
    @GetMapping("/status")
    public KnowledgeBaseStatus getKnowledgeStatus(@RequestParam("userId") Long userId) {
        String indexName = knowledgeIndexService.resolveIndexName(userId);
        KnowledgeBaseStatus status = new KnowledgeBaseStatus();
        status.setIndexName(indexName);
        status.setDocumentCount(0L);

        try {
            Request countRequest = new Request(HttpGet.METHOD_NAME, "/" + indexName + "/_count");
            Response countResponse = elasticsearchRestClient.performRequest(countRequest);
            JsonNode countJson = objectMapper.readTree(countResponse.getEntity().getContent());
            status.setDocumentCount(countJson.path("count").asLong(0L));

            Request searchRequest = new Request(HttpGet.METHOD_NAME, "/" + indexName + "/_search");
            searchRequest.setJsonEntity("{\"size\":5,\"query\":{\"match_all\":{}},\"_source\":true}");
            Response searchResponse = elasticsearchRestClient.performRequest(searchRequest);
            JsonNode hits = objectMapper.readTree(searchResponse.getEntity().getContent())
                    .path("hits")
                    .path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    String sample = extractSample(hit.path("_source"));
                    if (!sample.isBlank()) {
                        status.getSamples().add(sample);
                    }
                }
            }
        } catch (ResponseException e) {
            if (e.getMessage() == null || !e.getMessage().contains("index_not_found_exception")) {
                status.getSamples().add("Failed to read knowledge base status: " + safeMessage(e));
            }
        } catch (Exception e) {
            status.getSamples().add("Failed to read knowledge base status: " + safeMessage(e));
        }

        return status;
    }

    @Operation(summary = "Debug knowledge retrieval result for a specific query")
    @GetMapping("/debug-search")
    public KnowledgeSearchDebugView debugSearch(@RequestParam("userId") Long userId,
                                                 @RequestParam("query") String query) {
        return knowledgeSearchService.debugSearch(userId, query);
    }

    private DocumentParser createParser(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return new ApachePdfBoxDocumentParser();
        }
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return new ApacheTikaDocumentParser();
        }
        return new TextDocumentParser();
    }

    private List<TextSegment> buildSegments(String fileName, Document document, Long userId) {
        String cleanedText = cleanDocumentText(document.text());
        List<TextSegment> segments = new ArrayList<>();
        if (cleanedText.isBlank()) {
            return segments;
        }

        String normalizedName = fileName == null ? "unknown" : fileName.trim();
        for (String block : splitByStructure(cleanedText)) {
            for (String chunk : splitLongBlock(block)) {
                if (!chunk.isBlank()) {
                    segments.add(TextSegment.from("[userId=" + userId + "][source=" + normalizedName + "]\n" + chunk));
                }
            }
        }
        return segments;
    }

    private String cleanDocumentText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = rawText
                .replace('\u3000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');

        String[] lines = normalized.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = MULTI_SPACES.matcher(line).replaceAll(" ").trim();
            if (trimmed.isEmpty()) {
                builder.append('\n');
            } else {
                builder.append(trimmed).append('\n');
            }
        }

        return MULTI_NEWLINES.matcher(builder.toString().trim()).replaceAll("\n\n");
    }

    private List<String> splitByStructure(String cleanedText) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : cleanedText.split("\\n\\n")) {
            String value = paragraph.trim();
            if (value.isEmpty()) {
                continue;
            }

            boolean startsNewBlock = isHeading(value) || current.length() + value.length() > MAX_CHUNK_LENGTH;
            if (startsNewBlock && current.length() > 0) {
                blocks.add(current.toString().trim());
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(value);
        }

        if (current.length() > 0) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private List<String> splitLongBlock(String block) {
        List<String> chunks = new ArrayList<>();
        if (block.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(block);
            return chunks;
        }

        int start = 0;
        while (start < block.length()) {
            int end = Math.min(block.length(), start + MAX_CHUNK_LENGTH);
            if (end < block.length()) {
                int preferred = Math.max(
                        block.lastIndexOf('\n', end),
                        Math.max(block.lastIndexOf('。', end), block.lastIndexOf('.', end))
                );
                if (preferred > start + MIN_PREFERRED_SPLIT_DISTANCE) {
                    end = preferred + 1;
                }
            }

            String chunk = block.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= block.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private boolean isHeading(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return value.startsWith("#")
                || value.startsWith("##")
                || value.startsWith("###")
                || value.matches("^\\d+[.、].*")
                || lower.startsWith("q:")
                || lower.startsWith("a:")
                || value.length() <= 30;
    }

    private void ensureKnowledgeIndex(String indexName) {
        try {
            Request createRequest = new Request(HttpPut.METHOD_NAME, "/" + indexName);
            elasticsearchRestClient.performRequest(createRequest);
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("resource_already_exists_exception")) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    private String extractSample(JsonNode source) {
        if (source == null || source.isMissingNode() || source.isNull()) {
            return "";
        }

        JsonNode textNode = source.path("text");
        if (textNode.isMissingNode()) {
            textNode = source.path("content");
        }
        if (textNode.isMissingNode()) {
            textNode = source.path("textSegment").path("text");
        }

        String text = textNode.isMissingNode() ? source.toString() : textNode.asText("");
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > 300) {
            return text.substring(0, 300) + "...";
        }
        return text;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
