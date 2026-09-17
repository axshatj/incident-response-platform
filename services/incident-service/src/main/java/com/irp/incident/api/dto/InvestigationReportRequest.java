package com.irp.incident.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Posted by the agent-service after a bounded investigation. The incident-service
 * persists artifacts and does not invoke the LLM.
 */
public record InvestigationReportRequest(
        @NotNull UUID investigationId,
        @Size(max = 4096) String summary,
        @Valid List<ObservationRequest> observations,
        @Valid List<AgentRunRequest> agentRuns,
        @Valid RootCauseRequest rootCause
) {
    public record ObservationRequest(
            @NotBlank @Size(max = 64) String type,
            @NotBlank @Size(max = 128) String source,
            Instant observedAt,
            @NotBlank @Size(max = 4096) String content,
            Double confidence,
            @Size(max = 64) String traceId
    ) {}

    public record AgentRunRequest(
            @NotNull UUID id,
            @NotBlank @Size(max = 64) String agentType,
            @Size(max = 128) String model,
            @NotBlank @Size(max = 32) String status,
            Integer inputTokens,
            Integer outputTokens,
            Long latencyMs,
            @Size(max = 1024) String errorMessage,
            Instant startedAt,
            Instant completedAt,
            @Valid List<ToolCallRequest> toolCalls
    ) {}

    public record ToolCallRequest(
            @NotBlank @Size(max = 128) String toolName,
            @Size(max = 64) String argumentsHash,
            @NotBlank @Size(max = 32) String status,
            Long latencyMs,
            @Size(max = 2048) String resultPreview,
            Instant startedAt,
            Instant completedAt
    ) {}

    public record RootCauseRequest(
            @NotBlank @Size(max = 2048) String statement,
            @NotNull Double confidence,
            List<@Size(max = 512) String> evidence,
            List<@Size(max = 512) String> counterEvidence,
            List<@Size(max = 128) String> affectedComponents
    ) {}
}
