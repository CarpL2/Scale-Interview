package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.bean.UploadedMaterialsView;
import com.carp.ai.langchain4j.entity.InterviewRecord;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import com.carp.ai.langchain4j.service.UserInterviewProfileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "面试画像管理")
@RestController
@RequestMapping("/api/profile")
public class InterviewProfileController {

    @Resource
    private UserInterviewProfileService profileService;

    @Resource
    private com.carp.ai.langchain4j.service.InterviewRecordService interviewRecordService;

    @Operation(summary = "上传简历文件并解析（支持PDF, TXT, MD等）")
    @PostMapping("/resume/upload")
    public String uploadResume(@RequestParam("userId") Long userId, @RequestParam("sessionId") String sessionId, @RequestParam("file") MultipartFile file) {
        return processFileUpload(userId, sessionId, file, true);
    }

    @Operation(summary = "通过文件更新岗位JD（支持PDF, TXT, MD等）")
    @PostMapping("/jd/upload")
    public String uploadJD(@RequestParam("userId") Long userId, @RequestParam("sessionId") String sessionId, @RequestParam("file") MultipartFile file) {
        return processFileUpload(userId, sessionId, file, false);
    }

    @Operation(summary = "通过文本更新岗位JD")
    @PostMapping("/jd/update")
    public String updateJD(@RequestParam("userId") Long userId, @RequestParam("sessionId") String sessionId, @RequestBody String jdContent) {
        com.carp.ai.langchain4j.entity.InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, sessionId);
        record.setJdContent(jdContent);
        interviewRecordService.updateById(record);
        return "JD文本更新成功";
    }

    @Operation(summary = "更新面试风格 (PROFESSIONAL, STERN, ENCOURAGING)")
    @PostMapping("/style/update")
    public String updateStyle(@RequestParam("userId") Long userId, @RequestParam("sessionId") String sessionId, @RequestParam("style") String style) {
        com.carp.ai.langchain4j.entity.InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, sessionId);
        record.setInterviewStyle(style);
        interviewRecordService.updateById(record);
        return "面试风格更新成功";
    }

    @Operation(summary = "获取当前画像信息")
    @GetMapping("/{userId}")
    public UserInterviewProfile getProfile(@PathVariable("userId") Long userId) {
        return profileService.getByUserId(userId);
    }

    @Operation(summary = "查看当前会话已上传的简历和 JD 解析内容")
    @GetMapping("/materials")
    public UploadedMaterialsView getUploadedMaterials(@RequestParam("userId") Long userId,
                                                      @RequestParam("sessionId") String sessionId) {
        InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, sessionId);
        UploadedMaterialsView view = new UploadedMaterialsView();
        view.setSessionId(sessionId);
        if (record == null) {
            return view;
        }
        view.setResumeContent(record.getResumeContent());
        view.setResumeLength(lengthOf(record.getResumeContent()));
        view.setJdContent(record.getJdContent());
        view.setJdLength(lengthOf(record.getJdContent()));
        view.setInterviewStyle(record.getInterviewStyle());
        return view;
    }

    /**
     * 通用文件处理逻辑
     */
    private String processFileUpload(Long userId, String sessionId, MultipartFile file, boolean isResume) {
        if (file.isEmpty()) {
            return "文件为空";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) return "文件名不能为空";

        try {
            DocumentParser parser;
            if (fileName.toLowerCase().endsWith(".pdf")) {
                parser = new ApachePdfBoxDocumentParser();
            } else if (fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx")) {
                parser = new ApacheTikaDocumentParser();
            } else {
                // 默认使用文本解析器（支持 .txt, .md 等纯文本）
                parser = new TextDocumentParser();
            }

            Document document = parser.parse(file.getInputStream());
            String text = document.text();

            com.carp.ai.langchain4j.entity.InterviewRecord record = interviewRecordService.getOrCreateBySessionId(userId, sessionId);
            if (isResume) {
                record.setResumeContent(text);
            } else {
                record.setJdContent(text);
            }
            interviewRecordService.updateById(record);

            String type = isResume ? "简历" : "JD";
            return type + "文件上传并解析成功，字数：" + text.length();
        } catch (Exception e) {
            e.printStackTrace();
            return "解析失败：" + e.getMessage();
        }
    }

    private Integer lengthOf(String text) {
        return text == null ? 0 : text.length();
    }
}
