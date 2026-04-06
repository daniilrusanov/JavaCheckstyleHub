package com.checkstylehub.analyzer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RabbitMQ topology and JSON serialization for asynchronous repository analysis.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String ANALYSIS_QUEUE = "analysis_queue";
    public static final String ANALYSIS_EXCHANGE = "analysis.exchange";
    public static final String ANALYSIS_ROUTING_KEY = "analysis";

    @Bean
    public Queue analysisQueue() {
        return new Queue(ANALYSIS_QUEUE, true);
    }

    @Bean
    public DirectExchange analysisExchange() {
        return new DirectExchange(ANALYSIS_EXCHANGE, true, false);
    }

    @Bean
    public Binding analysisBinding(Queue analysisQueue, DirectExchange analysisExchange) {
        return BindingBuilder.bind(analysisQueue).to(analysisExchange).with(ANALYSIS_ROUTING_KEY);
    }

    @Bean
    @Primary
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Ensures {@link RabbitTemplate#convertAndSend} serializes
     * {@link com.checkstylehub.analyzer.dto.AnalysisQueueMessage} as JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        return template;
    }
}
