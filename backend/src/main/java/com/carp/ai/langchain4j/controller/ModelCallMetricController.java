package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.entity.ModelCallMetric;
import com.carp.ai.langchain4j.service.ModelCallMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "模型调用指标")
@RestController
@RequestMapping("/api/model-metrics")
public class ModelCallMetricController {

    @Resource
    private ModelCallMetricService modelCallMetricService;

    @Operation(summary = "查询当前用户最近模型调用记录")
    @GetMapping("/recent")
    public List<ModelCallMetric> recent(@RequestParam("userId") Long userId,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return modelCallMetricService.getRecentMetrics(userId, limit);
    }
}
