package com.checkstylehub.analyzer.config;

import com.checkstylehub.analyzer.dto.CachedAnalysisBundle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;

/**
 * Serializes {@link CachedAnalysisBundle} to JSON for Redis storage.
 */
public class CachedAnalysisBundleRedisSerializer implements RedisSerializer<CachedAnalysisBundle> {

    private final ObjectMapper objectMapper;
    private final JavaType bundleType;

    public CachedAnalysisBundleRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.bundleType = objectMapper.getTypeFactory().constructType(CachedAnalysisBundle.class);
    }

    @Override
    public byte[] serialize(CachedAnalysisBundle value) {
        if (value == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Failed to serialize cached analysis bundle", e);
        }
    }

    @Override
    public CachedAnalysisBundle deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, bundleType);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize cached analysis bundle", e);
        }
    }
}
