package com.carp.ai.langchain4j.bean;

import lombok.Data;

@Data
public class UploadedMaterialsView {
    private String sessionId;
    private String resumeContent;
    private Integer resumeLength;
    private String jdContent;
    private Integer jdLength;
    private String interviewStyle;
}
