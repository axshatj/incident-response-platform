package com.irp.agent.llm;

import com.irp.agent.observability.AgentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Every model output is parsed as JSON and bean-validated before use. Free text
 * is never used as control flow.
 */
@Component
public class StructuredLlm {

    private final ChatClient chatClient;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final AgentMetrics metrics;

    public StructuredLlm(ChatClient.Builder chatClientBuilder,
                         ObjectMapper mapper,
                         Validator validator,
                         AgentMetrics metrics) {
        this.chatClient = chatClientBuilder.build();
        this.mapper = mapper;
        this.validator = validator;
        this.metrics = metrics;
    }

    public <T> T generate(String system, String user, Class<T> type) {
        String agentType = inferAgent(system);
        long started = System.nanoTime();
        try {
            T value = generateUnchecked(system, user, type);
            metrics.recordLlm(agentType, true, (System.nanoTime() - started) / 1_000_000L);
            return value;
        } catch (RuntimeException e) {
            metrics.recordLlm(agentType, false, (System.nanoTime() - started) / 1_000_000L);
            throw e;
        }
    }

    private <T> T generateUnchecked(String system, String user, Class<T> type) {
        String raw = chatClient.prompt()
                .system(system)
                .user(user)
                .call()
                .content();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("LLM returned empty content for " + type.getSimpleName());
        }
        String json = extractJson(raw);
        try {
            T value = mapper.readValue(json, type);
            Set<ConstraintViolation<T>> violations = validator.validate(value);
            if (!violations.isEmpty()) {
                String detail = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));
                throw new IllegalArgumentException("LLM output failed schema validation: " + detail);
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("LLM output was not valid JSON for " + type.getSimpleName(), e);
        }
    }

    public static String inferAgent(String system) {
        if (system == null) {
            return "unknown";
        }
        if (system.contains("Triage Agent")) {
            return "TRIAGE";
        }
        if (system.contains("Root Cause Agent")) {
            return "RCA";
        }
        if (system.contains("Remediation Planning")) {
            return "REMEDIATION";
        }
        if (system.contains("Verification Agent")) {
            return "VERIFICATION";
        }
        if (system.contains("Investigation Agent")) {
            return "INVESTIGATION";
        }
        return "unknown";
    }

    public static String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new IllegalArgumentException("LLM output did not contain a JSON object");
    }
}
