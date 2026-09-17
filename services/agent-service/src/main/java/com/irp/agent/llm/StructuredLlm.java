package com.irp.agent.llm;

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

    public StructuredLlm(ChatClient.Builder chatClientBuilder,
                         ObjectMapper mapper,
                         Validator validator) {
        this.chatClient = chatClientBuilder.build();
        this.mapper = mapper;
        this.validator = validator;
    }

    public <T> T generate(String system, String user, Class<T> type) {
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

    static String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new IllegalArgumentException("LLM output did not contain a JSON object");
    }
}
