package com.checkstylehub.analyzer.config;

import com.checkstylehub.analyzer.dto.CachedAnalysisBundle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis {@link RedisTemplate} for caching aggregated analysis results and metrics.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CachedAnalysisBundle> analysisResultsRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, CachedAnalysisBundle> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        CachedAnalysisBundleRedisSerializer valueSerializer = new CachedAnalysisBundleRedisSerializer(objectMapper);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
