package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.config.RabbitMQConfig;
import com.checkstylehub.analyzer.dto.AnalysisQueueMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes analysis jobs from RabbitMQ and runs the repository analysis workflow.
 */
@Component
public class AnalysisMessageListener {

    private final AnalysisService analysisService;

    public AnalysisMessageListener(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_QUEUE)
    public void onAnalysisMessage(AnalysisQueueMessage message) {
        analysisService.startAnalysisFlow(message.getRequestId(), message.getCheckstyleConfig());
    }
}
