package com.carp.ai.langchain4j.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WeaknessProfileSanitizer {

    private static final int MAX_ITEM_LENGTH = 80;

    private WeaknessProfileSanitizer() {
    }

    public static List<String> sanitize(List<String> rawWeaknesses) {
        if (rawWeaknesses == null || rawWeaknesses.isEmpty()) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String raw : rawWeaknesses) {
            String cleaned = normalize(raw);
            if (cleaned.isBlank()) {
                continue;
            }
            if (isPromptLeak(cleaned)) {
                continue;
            }
            if (cleaned.length() > MAX_ITEM_LENGTH) {
                continue;
            }
            unique.add(cleaned);
        }
        return new ArrayList<>(unique);
    }

    public static String mergeToProfileText(String currentProfile, List<String> sanitizedWeaknesses) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(parseExisting(currentProfile));
        if (sanitizedWeaknesses != null) {
            merged.addAll(sanitizedWeaknesses);
        }

        if (merged.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String item : merged) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.toString().trim();
    }

    static List<String> parseExisting(String currentProfile) {
        if (currentProfile == null || currentProfile.isBlank()) {
            return List.of();
        }

        List<String> items = new ArrayList<>();
        String[] lines = currentProfile.split("\\R");
        for (String line : lines) {
            String normalized = normalize(line);
            if (normalized.startsWith("- ")) {
                normalized = normalized.substring(2).trim();
            }
            if (!normalized.isBlank() && !isPromptLeak(normalized)) {
                items.add(normalized);
            }
        }
        return items;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isPromptLeak(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("请提供")
                || lower.contains("辅导问答记录")
                || lower.contains("聊天记录")
                || lower.contains("以便我进行分析")
                || lower.contains("提炼出您的薄弱知识点")
                || lower.contains("请分析我们的聊天记录")
                || lower.contains("question-answer record");
    }
}
