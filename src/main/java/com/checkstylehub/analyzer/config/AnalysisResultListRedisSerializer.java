package com.checkstylehub.analyzer.config;

import com.checkstylehub.analyzer.entity.AnalysisResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.util.List;

/**
 * Serializes {@code List<AnalysisResult>} to JSON for Redis storage.
 */
public class AnalysisResultListRedisSerializer implements RedisSerializer<List<AnalysisResult>> {

    private final ObjectMapper objectMapper;
    private final JavaType listType;

    public AnalysisResultListRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, AnalysisResult.class);
    }

    @Override
    public byte[] serialize(List<AnalysisResult> value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize analysis results", e);
        }
    }

    @Override
    public List<AnalysisResult> deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, listType);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize analysis results", e);
        }
    }
}
