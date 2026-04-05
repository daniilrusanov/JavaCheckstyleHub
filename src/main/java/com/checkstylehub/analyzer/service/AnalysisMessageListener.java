package com.checkstylehub.analyzer.service;

import com.checkstylehub.analyzer.config.RabbitMQConfig;
import com.checkstylehub.analyzer.dto.AnalysisQueueMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumes analysis jobs from RabbitMQ and runs the repository analysis workflow.
 * Body is JSON text (compatible with {@link org.springframework.amqp.support.converter.SimpleMessageConverter}).
 */
@Component
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true")
public class AnalysisMessageListener {

    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper;

    public AnalysisMessageListener(AnalysisService analysisService, ObjectMapper objectMapper) {
        this.analysisService = analysisService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.ANALYSIS_QUEUE)
    public void onAnalysisMessage(String json) throws IOException {
        AnalysisQueueMessage message = objectMapper.readValue(json, AnalysisQueueMessage.class);
        analysisService.startAnalysisFlow(message.getRequestId(), message.getCheckstyleConfig());
    }
}
