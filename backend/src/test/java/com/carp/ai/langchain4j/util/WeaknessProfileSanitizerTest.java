package com.carp.ai.langchain4j.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaknessProfileSanitizerTest {

    @Test
    void shouldFilterPromptLeakAndDuplicateWeaknesses() {
        List<String> sanitized = WeaknessProfileSanitizer.sanitize(List.of(
                "Redis 持久化机制",
                "请提供您与导师的【辅导问答记录】，以便我进行分析并提炼出您的薄弱知识点。",
                "Redis 持久化机制",
                "JVM 垃圾回收"
        ));

        assertEquals(List.of("Redis 持久化机制", "JVM 垃圾回收"), sanitized);
    }

    @Test
    void shouldMergeExistingProfileWithoutKeepingDirtyLines() {
        String merged = WeaknessProfileSanitizer.mergeToProfileText(
                "- Redis 持久化机制\n- 请提供您与导师的【辅导问答记录】\n",
                List.of("并发编程基础", "Redis 持久化机制")
        );

        assertEquals("- Redis 持久化机制\n- 并发编程基础", merged);
    }
}
