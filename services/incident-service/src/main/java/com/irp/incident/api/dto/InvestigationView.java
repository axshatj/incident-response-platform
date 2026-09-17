package com.irp.incident.api.dto;

import com.irp.incident.domain.AgentRun;
import com.irp.incident.domain.Observation;
import com.irp.incident.domain.RootCause;
import com.irp.incident.domain.ToolCall;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationView(
        UUID incidentId,
        List<ObservationView> observations,
        List<AgentRunView> agentRuns,
        RootCauseView rootCause
) {
    public record ObservationView(
            UUID id,
            String type,
            String source,
            Instant observedAt,
            String content,
            Double confidence,
            String traceId
    ) {
        public static ObservationView from(Observation o) {
            return new ObservationView(o.getId(), o.getType(), o.getSource(), o.getObservedAt(),
                    o.getContent(), o.getConfidence(), o.getTraceId());
        }
    }

    public record AgentRunView(
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
            List<ToolCallView> toolCalls
    ) {
        public static AgentRunView from(AgentRun run, List<ToolCall> calls) {
            return new AgentRunView(
                    run.getId(), run.getAgentType(), run.getModel(), run.getStatus(),
                    run.getInputTokens(), run.getOutputTokens(), run.getLatencyMs(),
                    run.getErrorMessage(), run.getStartedAt(), run.getCompletedAt(),
                    calls.stream().map(ToolCallView::from).toList()
            );
        }
    }

    public record ToolCallView(
            UUID id,
            String toolName,
            String status,
            Long latencyMs,
            String resultPreview,
            Instant startedAt,
            Instant completedAt
    ) {
        public static ToolCallView from(ToolCall call) {
            return new ToolCallView(call.getId(), call.getToolName(), call.getStatus(),
                    call.getLatencyMs(), call.getResultPreview(), call.getStartedAt(), call.getCompletedAt());
        }
    }

    public record RootCauseView(
            UUID id,
            String statement,
            double confidence,
            String evidenceJson,
            String counterEvidenceJson,
            String affectedJson,
            Instant createdAt
    ) {
        public static RootCauseView from(RootCause rc) {
            return new RootCauseView(rc.getId(), rc.getStatement(), rc.getConfidence(),
                    rc.getEvidenceJson(), rc.getCounterEvidenceJson(), rc.getAffectedJson(), rc.getCreatedAt());
        }
    }
}
