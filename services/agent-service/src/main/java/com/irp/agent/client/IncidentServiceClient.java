package com.irp.agent.client;

import com.irp.agent.schema.RootCauseOutput;
import com.irp.agent.schema.TriageOutput;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
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

    public void advance(UUID id, String target, String eventType, String actor, String note) {
        http.post()
                .uri("/api/incidents/{id}/advance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "target", target,
                        "eventType", eventType,
                        "actor", actor,
                        "note", note == null ? "" : note
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public UUID startInvestigation(UUID incidentId) {
        Map<String, UUID> body = http.post()
                .uri("/api/incidents/{id}/investigations", incidentId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (body == null || body.get("investigationId") == null) {
            throw new IllegalStateException("incident-service did not return investigationId");
        }
        return body.get("investigationId");
    }

    public void postReport(UUID incidentId, InvestigationReport report) {
        http.post()
                .uri("/api/incidents/{id}/investigation-report", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(report)
                .retrieve()
                .toBodilessEntity();
    }

    public List<KnowledgeHitDto> searchKnowledge(String query, String service, String environment, int topK) {
        List<KnowledgeHitDto> hits = http.post()
                .uri("/api/knowledge/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "query", query,
                        "service", service == null ? "" : service,
                        "environment", environment == null ? "" : environment,
                        "topK", topK
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return hits == null ? List.of() : hits;
    }

    public void postRemediationPlan(UUID incidentId, RemediationPlanDto plan) {
        http.post()
                .uri("/api/incidents/{id}/remediation-plan", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(plan)
                .retrieve()
                .toBodilessEntity();
    }

    public record IncidentDto(
            UUID id,
            String externalId,
            String service,
            String title,
            String description,
            String severity,
            String status,
            String environment
    ) {}

    public record InvestigationReport(
            UUID investigationId,
            String summary,
            List<ObservationDto> observations,
            List<AgentRunDto> agentRuns,
            RootCauseDto rootCause
    ) {}

    public record ObservationDto(
            String type,
            String source,
            Instant observedAt,
            String content,
            Double confidence,
            String traceId
    ) {}

    public record AgentRunDto(
            UUID id,
            String agentType,
            String model,
            String status,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            String errorMessage,
            Instant startedAt,
            Instant completedAt,
            List<ToolCallDto> toolCalls
    ) {}

    public record ToolCallDto(
            String toolName,
            String argumentsHash,
            String status,
            Long latencyMs,
            String resultPreview,
            Instant startedAt,
            Instant completedAt
    ) {}

    public record RootCauseDto(
            String statement,
            Double confidence,
            List<String> evidence,
            List<String> counterEvidence,
            List<String> affectedComponents
    ) {
        public static RootCauseDto from(RootCauseOutput rca) {
            return new RootCauseDto(
                    rca.rootCause(),
                    rca.confidence(),
                    rca.evidence(),
                    rca.counterEvidence() == null ? List.of() : rca.counterEvidence(),
                    rca.affectedComponents()
            );
        }
    }

    public record KnowledgeHitDto(
            UUID id,
            String source,
            String path,
            String title,
            String service,
            String snippet,
            double score
    ) {
        public String citation() {
            return "[" + source + ":" + path + "] " + title;
        }
    }

    public record RemediationPlanDto(
            String action,
            String namespace,
            String deployment,
            Integer targetRevision,
            String expectedImpact,
            String blastRadius,
            Double confidence,
            String rationale
    ) {}

    public static String noteFor(TriageOutput triage) {
        return "suspected=" + String.join(",", triage.suspectedServices());
    }
}
