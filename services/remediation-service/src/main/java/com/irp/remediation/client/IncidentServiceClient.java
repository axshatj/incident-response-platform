package com.irp.remediation.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IncidentServiceClient {

    private final RestClient http;

    public IncidentServiceClient(RestClient incidentRestClient) {
        this.http = incidentRestClient;
    }

    public IncidentDto get(UUID id) {
        return http.get().uri("/api/incidents/{id}", id).retrieve().body(IncidentDto.class);
    }

    public RemediationView remediation(UUID id) {
        return http.get().uri("/api/incidents/{id}/remediation", id).retrieve().body(RemediationView.class);
    }

    public void recordExecution(UUID id, ExecutionRequest request) {
        http.post()
                .uri("/api/incidents/{id}/remediation-executions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncidentDto(UUID id, String service, String status, String environment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemediationView(UUID incidentId, PlanView plan, List<ExecutionView> executions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlanView(
            UUID id,
            String action,
            String namespace,
            String deployment,
            int targetRevision,
            String risk,
            String decision,
            String status,
            String idempotencyKey
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExecutionView(UUID id, String idempotencyKey, String status) {}

    public record ExecutionRequest(
            UUID planId,
            String idempotencyKey,
            String status,
            String resultPreview,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {}
}
