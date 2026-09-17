package com.irp.agent.eventing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.agent.orchestrator.InvestigationOrchestrator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentDetectedListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentDetectedListener.class);

    private final ObjectMapper mapper;
    private final InvestigationOrchestrator orchestrator;

    public IncidentDetectedListener(ObjectMapper mapper, InvestigationOrchestrator orchestrator) {
        this.mapper = mapper;
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "incident.detected", groupId = "${spring.kafka.consumer.group-id}")
    public void onDetected(String raw) {
        try {
            JsonNode root = mapper.readTree(raw);
            String type = root.path("eventType").asText();
            if (!"incident.detected".equals(type)) {
                log.warn("Ignoring unexpected eventType {}", type);
                return;
            }
            UUID incidentId = UUID.fromString(root.path("payload").path("incidentId").asText());
            orchestrator.handleDetected(incidentId);
        } catch (Exception e) {
            log.error("Failed to process incident.detected: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to process incident.detected", e);
        }
    }
}
