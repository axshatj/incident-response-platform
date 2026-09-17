package com.irp.agent.eventing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.agent.orchestrator.InvestigationOrchestrator;
import com.irp.agent.orchestrator.VerificationOrchestrator;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentUpdatedListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentUpdatedListener.class);

    private final ObjectMapper mapper;
    private final VerificationOrchestrator verification;
    private final InvestigationOrchestrator investigation;

    public IncidentUpdatedListener(ObjectMapper mapper,
                                   VerificationOrchestrator verification,
                                   InvestigationOrchestrator investigation) {
        this.mapper = mapper;
        this.verification = verification;
        this.investigation = investigation;
    }

    @KafkaListener(topics = "incident.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void onUpdated(String raw) {
        try {
            JsonNode root = mapper.readTree(raw);
            if (!"incident.updated".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");
            String to = payload.path("toStatus").asText();
            String lifecycle = payload.path("eventType").asText();
            UUID incidentId = UUID.fromString(payload.path("incidentId").asText());
            if ("VERIFYING".equals(to) && "REMEDIATION_EXECUTED".equals(lifecycle)) {
                verification.handleVerifying(incidentId);
            } else if ("INVESTIGATING".equals(to) && "VERIFICATION_RETRY".equals(lifecycle)) {
                investigation.handleRetry(incidentId);
            }
        } catch (Exception e) {
            log.error("Failed to process incident.updated: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to process incident.updated", e);
        }
    }
}
