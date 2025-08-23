package com.bank.wallet.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContextUtils {

    private final ObjectMapper objectMapper;

    public String getCurrentRequestMethod() {
        var request = getCurrentRequest();
        return request.getMethod();
    }

    public String getCurrentRequestPath() {
        var request = getCurrentRequest();
        return request.getRequestURI();
    }

    public String generateRequestHash(String method, String path, Object requestBody) {
        try {
            var bodyJson = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "";
            var input = method + path + bodyJson;

            return Hashing.sha256()
                    .hashString(input, StandardCharsets.UTF_8)
                    .toString();
        } catch (JsonProcessingException e) {
            log.error("Error generating request hash", e);
            throw new RuntimeException("Failed to generate request hash", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return Objects.requireNonNull(attributes.getRequest());
    }
}
