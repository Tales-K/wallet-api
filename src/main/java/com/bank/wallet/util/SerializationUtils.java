package com.bank.wallet.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SerializationUtils {

    private final ObjectMapper objectMapper;

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON", e);
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> objectToMap(Object object) {
        var json = toJson(object);
        return fromJson(json, Map.class);
    }

    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        var json = toJson(map); // garante reconstrução canônica
        return fromJson(json, clazz);
    }

    /**
     * Produz JSON canônico para armazenamento (normaliza via Map intermediário)
     */
    public String canonicalJson(Object object) {
        var map = objectToMap(object);
        return toJson(map);
    }
}

