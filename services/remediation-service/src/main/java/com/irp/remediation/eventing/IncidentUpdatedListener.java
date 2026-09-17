package com.irp.remediation.eventing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.remediation.executor.RemediationExecutor;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentUpdatedListener.class);

    private final ObjectMapper mapper;
    private final RemediationExecutor executor;

    public IncidentUpdatedListener(ObjectMapper mapper, RemediationExecutor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }

    @KafkaListener(topics = "incident.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void onUpdated(String raw) {
        try {
            JsonNode root = mapper.readTree(raw);
            if (!"incident.updated".equals(root.path("eventType").asText())) {
                return;
            }
            String to = root.path("payload").path("toStatus").asText();
            if (!"REMEDIATING".equals(to)) {
                return;
            }
            UUID incidentId = UUID.fromString(root.path("payload").path("incidentId").asText());
            executor.executeIfRemediating(incidentId);
        } catch (Exception e) {
            log.error("Failed to process incident.updated: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to process incident.updated", e);
        }
    }
}
