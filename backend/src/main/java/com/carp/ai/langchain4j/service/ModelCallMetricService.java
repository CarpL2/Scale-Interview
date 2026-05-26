package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.ModelCallMetric;

import java.util.List;

public interface ModelCallMetricService extends IService<ModelCallMetric> {

    long start();

    void recordSuccess(Long userId, String sessionId, String agentName, String modelName, boolean streaming, long startTime);

    void recordFailure(Long userId, String sessionId, String agentName, String modelName, boolean streaming, long startTime, Throwable error);

    List<ModelCallMetric> getRecentMetrics(Long userId, int limit);
}
